package com.minekube.craftless.driver.fabric.v1_21_6;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;

final class CraftlessMovementInput extends Input {
    private static final String PLAYER_INPUT_CLASS = "net.minecraft.util." + "Player" + "Input";
    private final Input delegate;
    private final boolean forward;
    private final boolean backward;
    private final boolean left;
    private final boolean right;
    private final boolean jump;
    private final boolean sneak;
    private final boolean sprint;
    private int ticks;
    private final Runnable restore;

    CraftlessMovementInput(
            Input delegate,
            boolean forward,
            boolean backward,
            boolean left,
            boolean right,
            boolean jump,
            boolean sneak,
            boolean sprint,
            int ticks,
            Runnable restore) {
        this.delegate = delegate;
        this.forward = forward;
        this.backward = backward;
        this.left = left;
        this.right = right;
        this.jump = jump;
        this.sneak = sneak;
        this.sprint = sprint;
        this.ticks = ticks;
        this.restore = restore;
    }

    public void tick() {
        tickCraftless(null, null);
    }

    public void tick(boolean slowDown, float slowDownFactor) {
        tickCraftless(slowDown, slowDownFactor);
    }

    private void tickCraftless(Boolean slowDown, Float slowDownFactor) {
        if (ticks <= 0) {
            restore.run();
            invokeDelegateTick(slowDown, slowDownFactor);
            copyKnownState(delegate, this);
            return;
        }

        applyIntent(this);
        ticks -= 1;
    }

    private void applyIntent(Input input) {
        setBoolean(input, "pressingForward", forward);
        setBoolean(input, "pressingBack", backward);
        setBoolean(input, "pressingLeft", left);
        setBoolean(input, "pressingRight", right);
        setBoolean(input, "jumping", jump);
        setBoolean(input, "sneaking", sneak);
        setFloat(input, "movementSideways", movementMultiplier(left, right));
        setFloat(input, "movementForward", movementMultiplier(forward, backward));
        setObject(input, "movementVector", movementVector());
        Object playerInput = newPlayerInput();
        if (playerInput != null) {
            setObject(input, "playerInput", playerInput);
        }
    }

    private void invokeDelegateTick(Boolean slowDown, Float slowDownFactor) {
        if (slowDown != null && invokeTick("tick", boolean.class, float.class, slowDown, slowDownFactor)) {
            return;
        }
        if (invokeTick("tick")) {
            return;
        }
        if (slowDown != null) {
            invokeTick("tick", boolean.class, float.class, slowDown, slowDownFactor);
        }
    }

    private boolean invokeTick(String name, Class<?> firstType, Class<?> secondType, Object first, Object second) {
        try {
            Method method = delegate.getClass().getMethod(name, firstType, secondType);
            method.invoke(delegate, first, second);
            return true;
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException
                | SecurityException ignored) {
            return false;
        }
    }

    private boolean invokeTick(String name) {
        try {
            Method method = delegate.getClass().getMethod(name);
            method.invoke(delegate);
            return true;
        } catch (NoSuchMethodException
                | IllegalAccessException
                | InvocationTargetException
                | SecurityException ignored) {
            return false;
        }
    }

    private Object newPlayerInput() {
        try {
            Class<?> playerInputClass = Class.forName(PLAYER_INPUT_CLASS);
            Constructor<?> constructor =
                    playerInputClass.getConstructor(
                            boolean.class,
                            boolean.class,
                            boolean.class,
                            boolean.class,
                            boolean.class,
                            boolean.class,
                            boolean.class);
            return constructor.newInstance(forward, backward, left, right, jump, sneak, sprint);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | InstantiationException
                | IllegalAccessException
                | InvocationTargetException
                | SecurityException
                | LinkageError ignored) {
            return null;
        }
    }

    private Vec2f movementVector() {
        return new Vec2f(movementMultiplier(left, right), movementMultiplier(forward, backward)).normalize();
    }

    private static float movementMultiplier(boolean positive, boolean negative) {
        if (positive == negative) {
            return 0.0f;
        }
        return positive ? 1.0f : -1.0f;
    }

    private static void copyKnownState(Input from, Input to) {
        copyField(from, to, "playerInput");
        copyField(from, to, "movementVector");
        copyField(from, to, "movementSideways");
        copyField(from, to, "movementForward");
        copyField(from, to, "pressingForward");
        copyField(from, to, "pressingBack");
        copyField(from, to, "pressingLeft");
        copyField(from, to, "pressingRight");
        copyField(from, to, "jumping");
        copyField(from, to, "sneaking");
    }

    private static void copyField(Object from, Object to, String name) {
        Field source = findField(from.getClass(), name);
        Field target = findField(to.getClass(), name);
        if (source == null || target == null) {
            return;
        }
        try {
            source.setAccessible(true);
            target.setAccessible(true);
            target.set(to, source.get(from));
        } catch (IllegalAccessException | SecurityException ignored) {
        }
    }

    private static void setBoolean(Object target, String name, boolean value) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.setBoolean(target, value);
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException ignored) {
        }
    }

    private static void setFloat(Object target, String name, float value) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.setFloat(target, value);
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException ignored) {
        }
    }

    private static void setObject(Object target, String name, Object value) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            return;
        }
        try {
            field.setAccessible(true);
            field.set(target, value);
        } catch (IllegalAccessException | IllegalArgumentException | SecurityException ignored) {
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            } catch (SecurityException ignored) {
                return null;
            }
        }
        return null;
    }
}
