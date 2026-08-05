package net.nostalgia.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import java.util.List;
import java.util.Random;

public final class PlasmaWireRenderer {

    public static class Spark {
        public float x, y, vx, vy;
        public int age, maxAge;
        public float scale;
    }

    public static class WireSegment {
        public final int x1, y1, x2, y2, length;
        public final boolean isVertical;
        public WireSegment(int x1, int y1, int x2, int y2) {
            this.x1 = x1; this.y1 = y1; this.x2 = x2; this.y2 = y2;
            this.isVertical = x1 == x2;
            this.length = this.isVertical ? Math.abs(y2 - y1) : Math.abs(x2 - x1);
        }
    }

    public static class WirePath {
        public final int width;
        public final List<WireSegment> segments;
        public final int totalLength;
        public WirePath(int width, List<WireSegment> segments) {
            this.width = width; this.segments = segments;
            int len = 0;
            for (WireSegment s : segments) len += s.length;
            this.totalLength = len;
        }
    }

    public static void drawWireBase(GuiGraphics graphics, WirePath path, int x, int y) {
        int wGutter = path.width + 2;
        for (WireSegment s : path.segments) {
            int x1 = x + s.x1, y1 = y + s.y1, x2 = x + s.x2, y2 = y + s.y2;
            int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
            int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
            if (s.isVertical) {
                graphics.fill(minX - wGutter/2, minY, minX - wGutter/2 + wGutter, maxY+1, 0xFF140D09);
                graphics.fill(minX + wGutter/2, minY, minX + wGutter/2 + 1, maxY+1, 0x22FFFFFF);
                graphics.fill(minX - wGutter/2 - 1, minY, minX - wGutter/2, maxY+1, 0x44000000);
            } else {
                graphics.fill(minX, minY - wGutter/2, maxX+1, minY - wGutter/2 + wGutter, 0xFF140D09);
                graphics.fill(minX, minY + wGutter/2, maxX+1, minY + wGutter/2 + 1, 0x22FFFFFF);
                graphics.fill(minX, minY - wGutter/2 - 1, maxX+1, minY - wGutter/2, 0x44000000);
            }
        }
    }

    public static void drawWirePathRange(GuiGraphics graphics, WirePath path, int x, int y, int d1, int d2, int width, int color) {
        if (d1 >= d2) return;
        int currentDist = 0;
        for (WireSegment s : path.segments) {
            int sEnd = currentDist + s.length;
            int s1 = Math.max(d1, currentDist), s2 = Math.min(d2, sEnd);
            if (s1 < s2) {
                int o1 = s1 - currentDist, o2 = s2 - currentDist;
                if (s.isVertical) {
                    int px = x + s.x1;
                    int py1 = y + s.y1 + (s.y2 >= s.y1 ? o1 : -o1);
                    int py2 = y + s.y1 + (s.y2 >= s.y1 ? o2 : -o2);
                    graphics.fill(px - width/2, Math.min(py1,py2), px - width/2 + width, Math.max(py1,py2)+1, color);
                } else {
                    int py = y + s.y1;
                    int px1 = x + s.x1 + (s.x2 >= s.x1 ? o1 : -o1);
                    int px2 = x + s.x1 + (s.x2 >= s.x1 ? o2 : -o2);
                    graphics.fill(Math.min(px1,px2), py - width/2, Math.max(px1,px2)+1, py - width/2 + width, color);
                }
            }
            currentDist += s.length;
        }
    }

    public static void drawCableLightning(GuiGraphics graphics, WirePath path, int x, int y, float time, int color) {
        long interval = (long)(time / 150.0f);
        Random rand = new Random(interval * 31 + path.hashCode());
        if (rand.nextFloat() > 0.4f) return;
        int segIdx = rand.nextInt(path.segments.size());
        WireSegment s = path.segments.get(segIdx);
        int px1 = x + s.x1, py1 = y + s.y1, px2 = x + s.x2, py2 = y + s.y2;
        int curX = px1, curY = py1;
        int steps = Math.max(3, s.length / 8);
        for (int i = 1; i <= steps; i++) {
            float t = (float)i/steps;
            int tx = (int)(px1 + (px2-px1)*t), ty = (int)(py1 + (py2-py1)*t);
            if (i < steps) { if (s.isVertical) tx += rand.nextInt(5)-2; else ty += rand.nextInt(5)-2; }
            int minX=Math.min(curX,tx),maxX=Math.max(curX,tx),minY=Math.min(curY,ty),maxY=Math.max(curY,ty);
            graphics.fill(minX-1,minY-1,maxX+2,maxY+2,(120<<24)|(color&0xFFFFFF));
            graphics.fill(minX,minY,maxX+1,maxY+1,0xFFFFFFFF);
            curX=tx; curY=ty;
        }
    }

    public static void drawSlotLightning(GuiGraphics graphics, int x, int y, float time, int color, float overloadProgress) {
        long interval = (long)(time / 80.0f);
        Random rand = new Random(interval * 17);
        int numArcs = overloadProgress > 0.6f ? rand.nextInt(3)+2 : rand.nextInt(2)+1;
        for (int a = 0; a < numArcs; a++) {
            int side = rand.nextInt(4);
            int sx = x + TimeMachineLayout.SLOT_X, sy = y + TimeMachineLayout.SLOT_Y;
            if (side==0) sx+=rand.nextInt(18); else if (side==1){sx+=rand.nextInt(18);sy+=18;} else if(side==2) sy+=rand.nextInt(18); else{sx+=18;sy+=rand.nextInt(18);}
            int angle=rand.nextInt(360); double rad=Math.toRadians(angle); int dist=rand.nextInt(12)+6;
            int ex=sx+(int)(Math.cos(rad)*dist),ey=sy+(int)(Math.sin(rad)*dist),curX=sx,curY=sy;
            for (int i=1;i<=3;i++) {
                float t=(float)i/3;
                int tx=(int)(sx+(ex-sx)*t+(rand.nextFloat()-0.5f)*4);
                int ty=(int)(sy+(ey-sy)*t+(rand.nextFloat()-0.5f)*4);
                if(i==3){tx=ex;ty=ey;}
                int minX=Math.min(curX,tx),maxX=Math.max(curX,tx),minY=Math.min(curY,ty),maxY=Math.max(curY,ty);
                graphics.fill(minX-1,minY-1,maxX+2,maxY+2,(100<<24)|(color&0xFFFFFF));
                graphics.fill(minX,minY,maxX+1,maxY+1,0xFFFFFFFF);
                curX=tx;curY=ty;
            }
        }
    }

    public static void drawPlasmaWire(GuiGraphics graphics, WirePath path, int x, int y, float time, float speed, int color, float flowProgress, float overloadProgress, boolean isOverloading) {
        int r=(color>>16)&0xFF,g=(color>>8)&0xFF,b=color&0xFF;
        int maxDist=(int)(path.totalLength*flowProgress);
        if(maxDist<=0) return;
        float alphaOuterVal=isOverloading?(0.12f+overloadProgress*0.15f+0.04f*(float)Math.sin(time*0.04f)):(0.08f+0.03f*(float)Math.sin(time*0.002f));
        int alphaOuter=Math.max(0,Math.min(255,(int)(alphaOuterVal*255)));
        drawWirePathRange(graphics,path,x,y,0,maxDist,path.width+4+(int)(overloadProgress*2),(alphaOuter<<24)|(r<<16)|(g<<8)|b);
        float alphaInnerVal=isOverloading?(0.25f+overloadProgress*0.30f+0.08f*(float)Math.sin(time*0.04f)):(0.22f+0.05f*(float)Math.sin(time*0.002f));
        int alphaInner=Math.max(0,Math.min(255,(int)(alphaInnerVal*255)));
        drawWirePathRange(graphics,path,x,y,0,maxDist,path.width+2+(int)(overloadProgress),(alphaInner<<24)|(r<<16)|(g<<8)|b);
        int alphaBody=(int)((isOverloading?1.0f:0.75f)*255);
        drawWirePathRange(graphics,path,x,y,0,maxDist,path.width,(alphaBody<<24)|(r<<16)|(g<<8)|b);
        int[] point=new int[2];
        for(int d=0;d<maxDist;d+=2) {
            getPointOnPath(path,d,point);
            int px=x+point[0],py=y+point[1];
            float phase1=d*0.15f-time*speed,phase2=d*0.35f-time*speed*1.6f;
            float wave1=(float)Math.sin(phase1)*0.45f+0.55f,wave2=(float)Math.sin(phase2)*0.25f+0.25f;
            float intensity=wave1+wave2;
            if(isOverloading) intensity*=(1.0f+overloadProgress*0.8f);
            int distToFront=maxDist-d;
            if(distToFront<6) intensity*=(distToFront/6.0f);
            intensity=Math.max(0,Math.min(2.5f,intensity));
            float alphaCoreVal=isOverloading?(0.6f+intensity*0.4f):(0.5f+intensity*0.3f);
            int alphaCore=Math.max(0,Math.min(255,(int)(alphaCoreVal*255)));
            int wCore=(intensity>1.1f)?path.width:Math.max(1,path.width-1);
            graphics.fill(px-wCore/2,py-wCore/2,px-wCore/2+wCore,py-wCore/2+wCore,(alphaCore<<24)|(r<<16)|(g<<8)|b);
            float whiteIntensity=intensity-(isOverloading?0.7f:0.9f);
            if(whiteIntensity>0) {
                int alphaWhite=Math.max(0,Math.min(255,(int)(whiteIntensity*1.5f*255)));
                graphics.fill(px,py,px+1,py+1,(alphaWhite<<24)|0xFFFFFF);
            }
        }
        if(maxDist>0) {
            getPointOnPath(path,maxDist,point);
            int px=x+point[0],py=y+point[1],rFront=path.width+1;
            graphics.fill(px-rFront/2,py-rFront/2,px-rFront/2+rFront,py-rFront/2+rFront,(180<<24)|(r<<16)|(g<<8)|b);
        }
    }

    public static void getPointOnPath(WirePath path, int dist, int[] outPoint) {
        int accumulated=0;
        for(WireSegment s:path.segments) {
            if(dist<=accumulated+s.length) {
                int sd=dist-accumulated;
                outPoint[0]=s.isVertical?s.x1:s.x1+(s.x2>=s.x1?sd:-sd);
                outPoint[1]=s.isVertical?s.y1+(s.y2>=s.y1?sd:-sd):s.y1;
                return;
            }
            accumulated+=s.length;
        }
        if(!path.segments.isEmpty()) { WireSegment last=path.segments.get(path.segments.size()-1); outPoint[0]=last.x2; outPoint[1]=last.y2; }
    }

    private PlasmaWireRenderer() {}
}
