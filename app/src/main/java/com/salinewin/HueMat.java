package com.salinewin;

public class HueMat {
    public static float[] build(float deg, float sat, float bri) {
        double rad = Math.toRadians(deg);
        double c = Math.cos(rad), s = Math.sin(rad);
        double u = 1.0/3.0, sw = Math.sqrt(u);

        double r00=u+c*(1-u), r01=u+c*(-u)+s*(-sw), r02=u+c*(-u)+s*sw;
        double r10=u+c*(-u)+s*sw, r11=u+c*(1-u), r12=u+c*(-u)+s*(-sw);
        double r20=u+c*(-u)+s*(-sw), r21=u+c*(-u)+s*sw, r22=u+c*(1-u);

        if (sat != 1.0f) {
            double lr=0.2126, lg=0.7152, lb=0.0722;
            r00=lerp(lr,r00,sat); r01=lerp(lg,r01,sat); r02=lerp(lb,r02,sat);
            r10=lerp(lr,r10,sat); r11=lerp(lg,r11,sat); r12=lerp(lb,r12,sat);
            r20=lerp(lr,r20,sat); r21=lerp(lg,r21,sat); r22=lerp(lb,r22,sat);
        }

        float b = bri;
        return new float[]{
            (float)(r00*b),(float)(r01*b),(float)(r02*b),0f,0f,
            (float)(r10*b),(float)(r11*b),(float)(r12*b),0f,0f,
            (float)(r20*b),(float)(r21*b),(float)(r22*b),0f,0f,
            0f,0f,0f,1f,0f
        };
    }
    private static double lerp(double a,double b,double t){return a+(b-a)*t;}
}
