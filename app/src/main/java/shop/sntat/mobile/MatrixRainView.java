package shop.sntat.mobile;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

/** Low-overhead animated matrix-like characters falling from the upper edge. */
public final class MatrixRainView extends View {
    private static final String LETTERS = "SNTAT0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint backdrop = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random();
    private float density;
    private int count;
    private float[] heads;
    private float[] speeds;
    private int[] lengths;
    private long previousTime = -1;

    public MatrixRainView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        paint.setTypeface(android.graphics.Typeface.MONOSPACE);
        paint.setTextSize(15 * density);
    }

    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        int columns = Math.max(1, Math.round(w / (20 * density)));
        if (columns == count && heads != null) return;
        count = columns;
        heads = new float[count];
        speeds = new float[count];
        lengths = new int[count];
        for (int i = 0; i < count; i++) resetColumn(i, h, true);
        backdrop.setShader(new LinearGradient(0, 0, 0, h,
                new int[]{Color.rgb(2, 16, 12), Color.rgb(6, 10, 25), Color.rgb(5, 7, 19)},
                null, Shader.TileMode.CLAMP));
        previousTime = -1;
    }

    private void resetColumn(int index, int screenHeight, boolean initial) {
        float row = 21 * density;
        heads[index] = initial ? -random.nextInt(Math.max(1, (int)(screenHeight * 1.3f)))
                : -random.nextInt(Math.max(1, (int)(row * 25)));
        speeds[index] = (85 + random.nextInt(145)) * density;
        lengths[index] = 6 + random.nextInt(14);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawRect(0, 0, getWidth(), getHeight(), backdrop);
        long now = android.os.SystemClock.uptimeMillis();
        float elapsed = previousTime < 0 ? 0 : Math.min(50, now - previousTime) / 1000f;
        previousTime = now;
        float spacing = getWidth() / (float) count;
        float row = 21 * density;
        for (int i = 0; i < count; i++) {
            heads[i] += speeds[i] * elapsed;
            if (heads[i] - lengths[i] * row > getHeight()) resetColumn(i, getHeight(), false);
            for (int j = 0; j < lengths[i]; j++) {
                float y = heads[i] - row * j;
                if (y < 0 || y > getHeight() + row) continue;
                int alpha = Math.max(12, (int)(105 * (1f - j / (float)lengths[i])));
                if (j == 0) paint.setColor(Color.argb(180, 194, 255, 194));
                else paint.setColor(Color.argb(alpha, 29, 246, 112));
                // A new character periodically keeps the streams alive without assets or network.
                char symbol = LETTERS.charAt((i * 7 + j * 11 + (int)(now / 115)) % LETTERS.length());
                canvas.drawText(String.valueOf(symbol), i * spacing, y, paint);
            }
        }
        postInvalidateDelayed(45);
    }

    public void stop() { previousTime = -1; }
}
