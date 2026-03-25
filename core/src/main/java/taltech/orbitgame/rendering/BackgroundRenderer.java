package taltech.orbitgame.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public final class BackgroundRenderer {
    private BackgroundRenderer() {
    }

    public static void drawFit(SpriteBatch batch, Texture texture) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float texW = texture.getWidth();
        float texH = texture.getHeight();

        float scale = Math.min(screenW / texW, screenH / texH);
        float drawW = texW * scale;
        float drawH = texH * scale;
        float x = (screenW - drawW) / 2f;
        float y = (screenH - drawH) / 2f;

        batch.draw(texture, x, y, drawW, drawH);
    }

    public static void drawCover(SpriteBatch batch, Texture texture) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float texW = texture.getWidth();
        float texH = texture.getHeight();

        float scale = Math.max(screenW / texW, screenH / texH);
        float drawW = texW * scale;
        float drawH = texH * scale;
        float x = (screenW - drawW) / 2f;
        float y = (screenH - drawH) / 2f;

        batch.draw(texture, x, y, drawW, drawH);
    }
}
