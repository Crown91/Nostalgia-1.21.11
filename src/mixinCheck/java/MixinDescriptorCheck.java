import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Validates every injector declared by the mixins listed in a mixin config
 * against the real classes they target.
 *
 * <p>Usage:
 * {@code MixinDescriptorCheck <config.json> <modClassesPath> <compileClasspath> <reportFile> [strict]}
 *
 * <p>Everything here works on compiled bytecode with Mojang (named) mappings,
 * which is exactly the state of both the mod classes and the Minecraft jar
 * before {@code remapJar} runs, so annotation strings and target classes are
 * directly comparable.
 */
public final class MixinDescriptorCheck {

    private static final String CI = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CIR = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    private static final String MIXIN_ANN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW_ANN = "Lorg/spongepowered/asm/mixin/Shadow;";

    private static final List<Path> dirs = new ArrayList<>();
    private static final List<ZipFile> jars = new ArrayList<>();
    private static final Map<String, ClassNode> cache = new HashMap<>();
    private static final Set<String> reportedMissingTargets = new LinkedHashSet<>();
    private static final List<String> report = new ArrayList<>();

    private static int errors;
    private static int warnings;
    private static int verified;

    public static void main(String[] args) throws Exception {
        if (args.length < 4) {
            System.err.println("usage: MixinDescriptorCheck <config.json> <modClasses> <compileClasspath> <report> [strict]");
            System.exit(2);
        }
        Path configFile = Paths.get(args[0]);
        addToSearchPath(args[1]);
        addToSearchPath(args[2]);
        Path reportFile = Paths.get(args[3]);
        boolean strict = args.length > 4 && Boolean.parseBoolean(args[4]);

        String json = new String(Files.readAllBytes(configFile), StandardCharsets.UTF_8);
        String pkg = firstGroup(json, "\"package\"\\s*:\\s*\"([^\"]+)\"");
        if (pkg == null) {
            throw new IllegalStateException("no \"package\" entry in " + configFile);
        }

        LinkedHashSet<String> entries = new LinkedHashSet<>();
        entries.addAll(stringArray(json, "mixins"));
        entries.addAll(stringArray(json, "client"));
        entries.addAll(stringArray(json, "server"));

        line("mixin config : " + configFile);
        line("package      : " + pkg);
        line("entries      : " + entries.size());
        line("");

        for (String entry : entries) {
            String internal = (pkg + '.' + entry).replace('.', '/');
            ClassNode mixin = load(internal);
            if (mixin == null) {
                error(entry, "listed in the config but no compiled class was found at " + internal + ".class");
                continue;
            }

            List<String> targets = targetsOf(mixin);
            if (targets == null) {
                error(entry, "listed in the config but the class has no @Mixin annotation - Mixin throws "
                        + "\"is missing an @Mixin annotation\" during PREPARE and the game dies before the main menu");
                continue;
            }
            if (targets.isEmpty()) {
                warn(entry, "@Mixin declares no targets");
                continue;
            }

            List<ClassNode> targetNodes = new ArrayList<>();
            for (String target : targets) {
                ClassNode node = load(target);
                if (node == null) {
                    if (reportedMissingTargets.add(target)) {
                        warn(entry, "@Mixin target " + target.replace('/', '.')
                                + " is not on the compile classpath - this mixin will silently never apply");
                    }
                } else {
                    targetNodes.add(node);
                }
            }
            if (targetNodes.isEmpty()) {
                continue;
            }

            for (MethodNode method : mixin.methods) {
                if (method.visibleAnnotations == null) {
                    continue;
                }
                for (AnnotationNode ann : method.visibleAnnotations) {
                    if (SHADOW_ANN.equals(ann.desc)) {
                        checkShadowMethod(entry, method, targetNodes);
                        continue;
                    }
                    Injector injector = injectorOf(ann);
                    if (injector == null) {
                        continue;
                    }
                    for (String selector : injector.selectors) {
                        checkSelector(entry, method, targetNodes, selector, injector);
                    }
                }
            }

            for (FieldNode field : mixin.fields) {
                if (field.visibleAnnotations == null) {
                    continue;
                }
                for (AnnotationNode ann : field.visibleAnnotations) {
                    if (SHADOW_ANN.equals(ann.desc)) {
                        checkShadowField(entry, field, targetNodes);
                    }
                }
            }
        }

        line("");
        line("verified injectors : " + verified);
        line("errors             : " + errors);
        line("warnings           : " + warnings);

        if (reportFile.getParent() != null) {
            Files.createDirectories(reportFile.getParent());
        }
        Files.write(reportFile, report);
        for (String l : report) {
            System.out.println(l);
        }

        if (errors > 0) {
            System.out.println();
            System.out.println("mixin descriptor check found " + errors
                    + " problem(s) that will crash the game at runtime; full report: " + reportFile);
            if (strict) {
                System.exit(1);
            }
        }
        closeJars();
    }

    // ---------------------------------------------------------------- checks

    private static void checkSelector(String entry, MethodNode handler, List<ClassNode> targets,
            String rawSelector, Injector injector) {
        String selector = rawSelector.trim();
        if (selector.isEmpty() || selector.indexOf('*') >= 0 || selector.startsWith("@")) {
            return; // wildcard / dynamic selectors are out of scope
        }
        // fully qualified form: Lowner/Name;method(desc)ret
        if (selector.startsWith("L") && selector.indexOf(';') > 0) {
            selector = selector.substring(selector.indexOf(';') + 1);
        }
        String name = selector;
        String desc = null;
        int paren = selector.indexOf('(');
        if (paren >= 0) {
            name = selector.substring(0, paren);
            desc = selector.substring(paren);
        }

        List<MethodNode> byName = new ArrayList<>();
        List<MethodNode> exact = new ArrayList<>();
        for (ClassNode target : targets) {
            for (MethodNode candidate : target.methods) {
                if (!candidate.name.equals(name)) {
                    continue;
                }
                byName.add(candidate);
                if (desc == null || candidate.desc.equals(desc)) {
                    exact.add(candidate);
                }
            }
        }

        if (byName.isEmpty()) {
            finding(entry, injector, "@" + injector.kind + " " + handler.name + ": target method '" + name
                    + "' does not exist in " + names(targets)
                    + " - at runtime this is \"failed injection check, Scanned 0 target(s)\"");
            return;
        }
        if (exact.isEmpty()) {
            finding(entry, injector, "@" + injector.kind + " " + handler.name + ": selector '" + rawSelector.trim()
                    + "' matches no overload; '" + name + "' exists as " + descriptors(byName));
            return;
        }
        if (!injector.isInject) {
            verified++;
            return;
        }

        Type[] handlerArgs = Type.getArgumentTypes(handler.desc);
        int callbackIndex = -1;
        for (int i = 0; i < handlerArgs.length; i++) {
            if (handlerArgs[i].getSort() == Type.OBJECT) {
                String internal = handlerArgs[i].getInternalName();
                if (CI.equals(internal) || CIR.equals(internal)) {
                    callbackIndex = i;
                    break;
                }
            }
        }
        if (callbackIndex < 0) {
            finding(entry, injector, "@Inject " + handler.name
                    + ": handler has no CallbackInfo/CallbackInfoReturnable parameter");
            return;
        }

        Type[] provided = Arrays.copyOfRange(handlerArgs, 0, callbackIndex);
        for (MethodNode candidate : exact) {
            Type[] expected = Type.getArgumentTypes(candidate.desc);
            // Mixin accepts either no target args at all, or all of them verbatim.
            if (provided.length == 0 || Arrays.equals(provided, expected)) {
                verified++;
                return;
            }
        }

        finding(entry, injector, "@Inject " + handler.name + " into " + name + ": expected "
                + join(Type.getArgumentTypes(exact.get(0).desc)) + " but found " + join(provided)
                + " - Mixin fails APPLY with InvalidInjectionException");
    }

    private static void checkShadowMethod(String entry, MethodNode method, List<ClassNode> targets) {
        String name = method.name.startsWith("shadow$") ? method.name.substring("shadow$".length()) : method.name;
        for (ClassNode target : targets) {
            for (ClassNode node : withSupers(target)) {
                for (MethodNode candidate : node.methods) {
                    if (candidate.name.equals(name) && candidate.desc.equals(method.desc)) {
                        return;
                    }
                }
            }
        }
        warn(entry, "@Shadow method " + name + method.desc + " not found in " + names(targets) + " or its supertypes");
    }

    private static void checkShadowField(String entry, FieldNode field, List<ClassNode> targets) {
        String name = field.name.startsWith("shadow$") ? field.name.substring("shadow$".length()) : field.name;
        for (ClassNode target : targets) {
            for (ClassNode node : withSupers(target)) {
                for (FieldNode candidate : node.fields) {
                    if (candidate.name.equals(name) && candidate.desc.equals(field.desc)) {
                        return;
                    }
                }
            }
        }
        warn(entry, "@Shadow field " + name + " " + field.desc + " not found in " + names(targets) + " or its supertypes");
    }

    // ----------------------------------------------------------- annotations

    private static List<String> targetsOf(ClassNode mixin) {
        if (mixin.visibleAnnotations == null) {
            return null;
        }
        for (AnnotationNode ann : mixin.visibleAnnotations) {
            if (!MIXIN_ANN.equals(ann.desc)) {
                continue;
            }
            List<String> targets = new ArrayList<>();
            if (ann.values != null) {
                for (int i = 0; i + 1 < ann.values.size(); i += 2) {
                    Object key = ann.values.get(i);
                    Object value = ann.values.get(i + 1);
                    if ("value".equals(key) && value instanceof List) {
                        for (Object item : (List<?>) value) {
                            if (item instanceof Type) {
                                targets.add(((Type) item).getInternalName());
                            }
                        }
                    } else if ("targets".equals(key) && value instanceof List) {
                        for (Object item : (List<?>) value) {
                            targets.add(String.valueOf(item).replace('.', '/'));
                        }
                    }
                }
            }
            return targets;
        }
        return null;
    }

    private static Injector injectorOf(AnnotationNode ann) {
        String desc = ann.desc;
        if (desc == null) {
            return null;
        }
        boolean sponge = desc.startsWith("Lorg/spongepowered/asm/mixin/injection/");
        boolean extras = desc.startsWith("Lcom/llamalad7/mixinextras/injector/");
        if (!sponge && !extras) {
            return null;
        }
        String kind = desc.substring(desc.lastIndexOf('/') + 1, desc.length() - 1);
        List<String> selectors = new ArrayList<>();
        int require = -1;
        if (ann.values != null) {
            for (int i = 0; i + 1 < ann.values.size(); i += 2) {
                Object key = ann.values.get(i);
                Object value = ann.values.get(i + 1);
                if ("method".equals(key)) {
                    if (value instanceof List) {
                        for (Object item : (List<?>) value) {
                            selectors.add(String.valueOf(item));
                        }
                    } else {
                        selectors.add(String.valueOf(value));
                    }
                } else if ("require".equals(key) && value instanceof Integer) {
                    require = (Integer) value;
                }
            }
        }
        if (selectors.isEmpty()) {
            return null;
        }
        return new Injector(kind, "Inject".equals(kind), selectors, require);
    }

    private static final class Injector {
        final String kind;
        final boolean isInject;
        final List<String> selectors;
        final int require;

        Injector(String kind, boolean isInject, List<String> selectors, int require) {
            this.kind = kind;
            this.isInject = isInject;
            this.selectors = selectors;
            this.require = require;
        }
    }

    // -------------------------------------------------------- class loading

    private static void addToSearchPath(String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        for (String element : path.split(Pattern.quote(File.pathSeparator))) {
            if (element.isEmpty()) {
                continue;
            }
            File file = new File(element);
            if (!file.exists()) {
                continue;
            }
            if (file.isDirectory()) {
                dirs.add(file.toPath());
            } else if (element.endsWith(".jar") || element.endsWith(".zip")) {
                try {
                    jars.add(new ZipFile(file));
                } catch (Exception ignored) {
                    // a broken or unreadable jar is not this tool's problem
                }
            }
        }
    }

    private static ClassNode load(String internalName) {
        if (cache.containsKey(internalName)) {
            return cache.get(internalName);
        }
        byte[] bytes = read(internalName + ".class");
        ClassNode node = null;
        if (bytes != null) {
            node = new ClassNode();
            new ClassReader(bytes).accept(node,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        }
        cache.put(internalName, node);
        return node;
    }

    private static byte[] read(String resource) {
        for (Path dir : dirs) {
            Path candidate = dir.resolve(resource);
            if (Files.isRegularFile(candidate)) {
                try {
                    return Files.readAllBytes(candidate);
                } catch (Exception ignored) {
                    // fall through
                }
            }
        }
        for (ZipFile jar : jars) {
            ZipEntry zipEntry = jar.getEntry(resource);
            if (zipEntry == null) {
                continue;
            }
            try (InputStream in = jar.getInputStream(zipEntry)) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) > 0) {
                    out.write(buffer, 0, read);
                }
                return out.toByteArray();
            } catch (Exception ignored) {
                // fall through
            }
        }
        return null;
    }

    private static List<ClassNode> withSupers(ClassNode node) {
        List<ClassNode> chain = new ArrayList<>();
        ClassNode current = node;
        int guard = 0;
        while (current != null && guard++ < 32) {
            chain.add(current);
            for (String itf : current.interfaces) {
                ClassNode loaded = load(itf);
                if (loaded != null) {
                    chain.add(loaded);
                }
            }
            current = current.superName == null ? null : load(current.superName);
        }
        return chain;
    }

    private static void closeJars() {
        for (ZipFile jar : jars) {
            try {
                jar.close();
            } catch (Exception ignored) {
                // nothing useful to do here
            }
        }
    }

    // ---------------------------------------------------------------- output

    private static void finding(String entry, Injector injector, String message) {
        if (injector.require == 0) {
            warn(entry, message + " [require = 0, so this only disables the feature]");
        } else {
            error(entry, message);
        }
    }

    private static void error(String entry, String message) {
        errors++;
        line("ERROR " + entry + ": " + message);
    }

    private static void warn(String entry, String message) {
        warnings++;
        line("WARN  " + entry + ": " + message);
    }

    private static void line(String text) {
        report.add(text);
    }

    // ---------------------------------------------------------------- format

    private static String names(List<ClassNode> targets) {
        StringBuilder sb = new StringBuilder();
        for (ClassNode target : targets) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(target.name.replace('/', '.'));
        }
        return sb.toString();
    }

    private static String descriptors(List<MethodNode> methods) {
        StringBuilder sb = new StringBuilder();
        for (MethodNode method : methods) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(method.name).append(method.desc);
        }
        return sb.toString();
    }

    private static String join(Type[] types) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < types.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(simple(types[i]));
        }
        return sb.append(')').toString();
    }

    private static String simple(Type type) {
        String name = type.getClassName();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? name : name.substring(dot + 1);
    }

    private static String firstGroup(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1) : null;
    }

    private static List<String> stringArray(String json, String key) {
        List<String> values = new ArrayList<>();
        Matcher matcher = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\\[").matcher(json);
        if (!matcher.find()) {
            return values;
        }
        int end = json.indexOf(']', matcher.end());
        if (end < 0) {
            return values;
        }
        Matcher items = Pattern.compile("\"([^\"]+)\"").matcher(json.substring(matcher.end(), end));
        while (items.find()) {
            values.add(items.group(1));
        }
        return values;
    }

    private MixinDescriptorCheck() {
    }
}
