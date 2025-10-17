package entity;

import java.util.WeakHashMap;
import system.Config;

// BaseAttack — คลาสแม่ของพฤติกรรมการโจมตี ใช้แชร์สถานะและตัวช่วยร่วมกัน
abstract class BaseAttack<T extends BaseAttack.State> implements Monster.AttackBehavior {

    protected static class State {
        String currentAnimation = "";
        int frameIndex;
        int frameTimer;
        boolean animationFinished;
        long lastActionTime = System.currentTimeMillis();

        void resetFrames() {
            frameIndex = 0;
            frameTimer = 0;
            animationFinished = false;
        }
    }

    private final WeakHashMap<Monster, T> states = new WeakHashMap<>();

    protected abstract T createState();

    protected final T state(Monster self) {
        return states.computeIfAbsent(self, s -> createState());
    }

    protected final void clearState(Monster self) {
        states.remove(self);
    }

    protected final void switchAnimation(Monster self, T data, String animation) {
        if (animation.equals(data.currentAnimation)) return;
        self.setAnimation(animation);
        data.currentAnimation = animation;
        data.resetFrames();
    }

    protected final boolean advanceAnimation(T data, int totalFrames) {
        return advanceAnimation(data, totalFrames, Config.FRAME_DELAY_MONSTER);
    }

    protected final boolean advanceAnimation(T data, int totalFrames, int delay) {
        if (totalFrames <= 0 || data.animationFinished) return true;
        if (++data.frameTimer < delay) return false;

        data.frameTimer = 0;
        if (++data.frameIndex < totalFrames) return false;

        data.frameIndex = totalFrames - 1;
        data.animationFinished = true;
        return true;
    }

    protected final void resetAnimationState(T data) {
        data.resetFrames();
    }

    protected final boolean cooldownReady(T data, long cooldownMs) {
        return System.currentTimeMillis() - data.lastActionTime >= cooldownMs;
    }

    protected final void markCooldown(T data) {
        data.lastActionTime = System.currentTimeMillis();
    }
}