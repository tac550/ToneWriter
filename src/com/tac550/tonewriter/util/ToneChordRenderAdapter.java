package com.tac550.tonewriter.util;

import com.tac550.tonewriter.io.LilyPondInterface;
import com.tac550.tonewriter.model.Chord;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class ToneChordRenderAdapter {

    public static class DoneSignal {
        public final Lock lock;
        public final Condition cond;

        public DoneSignal() {
            lock = new ReentrantLock();
            cond = lock.newCondition();
        }
    }

    private static final Map<String, File[]> uniqueChordRenders = new HashMap<>();
    private static final Lock renderMapLock = new ReentrantLock();
    private static final Map<String, DoneSignal> uniqueChordSig = new HashMap<>();
    private static final Lock sigMapLock = new ReentrantLock();

    public static String generateChordId(Chord chord, String key_sig) {
        return chord.getFields().replace("<", "(").replace(">", ")") + "-"
                + TWUtils.convertAccidentalSymbols(key_sig);
    }

    public static void renderChord(Chord chord, String key_sig, Consumer<File[]> exit_actions) {
        Renderer renderer = new Renderer(chord, key_sig);
        Thread thread = new Thread(renderer);
        thread.start();

        new Thread(() -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            exit_actions.accept(renderer.getResults());
        }).start();
    }

    public static class Renderer implements Runnable {
        private File[] results;

        private final Chord chord;
        private final String key_sig;

        public Renderer(Chord chord, String key_sig) {
            this.chord = chord;
            this.key_sig = key_sig;
        }

        @Override
        public void run() {
            final String chordID = generateChordId(chord, key_sig);

            renderMapLock.lock();
            if (!uniqueChordRenders.containsKey(chordID)) {
                uniqueChordRenders.put(chordID, null);
                sigMapLock.lock();
                uniqueChordSig.put(chordID, new DoneSignal());
                sigMapLock.unlock();
                renderMapLock.unlock();
                try {
                    LilyPondInterface.renderChord(chord, key_sig, (files -> {
                        renderMapLock.lock();
                        uniqueChordRenders.put(chordID, files);
                        renderMapLock.unlock();
                        DoneSignal sig = uniqueChordSig.get(chordID);
                        sig.lock.lock();
                        sig.cond.signalAll();
                        sig.lock.unlock();
                    }));
                    uniqueChordSig.get(chordID).lock.lock();
                    uniqueChordSig.get(chordID).cond.await();
                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    uniqueChordSig.get(chordID).lock.unlock();
                }
            } else if (uniqueChordRenders.get(chordID) == null) {
                renderMapLock.unlock();
                uniqueChordSig.get(chordID).lock.lock();
                try {
                    uniqueChordSig.get(chordID).cond.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    uniqueChordSig.get(chordID).lock.unlock();
                }
            } else {
                renderMapLock.unlock();
            }

            renderMapLock.lock();
            results = uniqueChordRenders.get(chordID);
            renderMapLock.unlock();
        }

        public File[] getResults() {
            return results;
        }

    }
}
