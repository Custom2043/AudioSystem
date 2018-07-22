package audio;

public class Fading
{
    public final int duration, player;
    public final float baseVolumeL, baseVolumeR, endVolumeL, endVolumeR;
    public int remaining;

    public Fading(int p, int d, float b, float e)
    {
        this(p, d, b, b, e, e);
    }
    public Fading(int p, int d, float bL, float bR, float eL, float eR)
    {
        this.player = p;
        this.remaining = this.duration = d;
        this.baseVolumeL = bL;
        this.baseVolumeR = bR;
        this.endVolumeL = eL;
        this.endVolumeR = eR;
    }
}
