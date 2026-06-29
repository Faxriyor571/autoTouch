package core;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;

import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HotkeyService implements NativeKeyListener {

    private Runnable          onF2;
    private Consumer<Integer> uiCallback;

    public void start(Runnable onF2) {
        this.onF2 = onF2;

        Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());
        logger.setLevel(Level.OFF);
        logger.setUseParentHandlers(false);

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(this);
            System.out.println("[HOOK] GlobalScreen muvaffaqiyatli ulandi. F2 = yangi nuqta");
        } catch (NativeHookException e) {
            System.err.println("[HOOK] GlobalScreen ulanishda XATO: " + e.getMessage());
        }
    }

    public void setUiCallback(Consumer<Integer> uiCallback) {
        this.uiCallback = uiCallback;
        System.out.println("[HOOK] UI callback o'rnatildi");
    }

    public void stop() {
        try {
            GlobalScreen.removeNativeKeyListener(this);
            GlobalScreen.unregisterNativeHook();
            System.out.println("[HOOK] GlobalScreen to'xtatildi");
        } catch (NativeHookException e) {
            System.err.println("[HOOK] To'xtatishda xato: " + e.getMessage());
        }
        onF2       = null;
        uiCallback = null;
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (onF2 == null) return;
        if (e.getKeyCode() != NativeKeyEvent.VC_F2) return;

        System.out.println("[HOOK] F2 bosildi -- yangi nuqta qo'shilmoqda");

        onF2.run();

        if (uiCallback != null) {
            javax.swing.SwingUtilities.invokeLater(() -> uiCallback.accept(0));
        }
    }

    @Override public void nativeKeyReleased(NativeKeyEvent e) {}
    @Override public void nativeKeyTyped(NativeKeyEvent e) {}
}
