package scorched.android;

import java.io.IOException;
import java.util.HashSet;

import android.content.Context;
import android.media.MediaPlayer;

/** Singleton class for sounds.  Using an enum means that we won't serialize
 * the class incorrectly (though there is no reason to serialize it) and means
 * that we are immune to reflection attacks (though there is no security risk
 * from a reflection attack here).
 */
public enum Sound  {
    instance;

    /** Encapsulation of the state of the Sound singleton.  This is necessary
     * in order to store the state in a volatile reference which can be
     * consistently DCL'ed.
     */
    private static class State implements MediaPlayer.OnCompletionListener {
        // Put objects here for each distinct sound

        /** The sound of an explosion */
        public final MediaPlayer boom;

        /** Set of sounds which are playing or not ready to be played again */
        public final HashSet<MediaPlayer> playing;

        /** Clean up the sound for future plays. */
        public void onCompletion(MediaPlayer sound) {
            synchronized (playing) {
                // Even though the sound has completed, we must stop() it
                // explicitly or an IllegalStateException is thrown
                sound.stop();
                try {
                    sound.prepare();
                } catch (IOException e) {
                    // This can't happen...
                    assert(false);
                }
                assert(playing.contains(sound));
                playing.remove(sound);
            }
        }

        /** Play a sound - don't let it play twice at once. */
        private void playSound(MediaPlayer sound) {
            synchronized (playing) {
                if (playing.contains(sound)) {
                    return;
                }
                playing.add(sound);
                sound.start();
            }
        }

        public State(Context context) {
            boom = MediaPlayer.create(context, R.raw.boom);
            boom.setOnCompletionListener(this);
            playing = new HashSet<MediaPlayer>();
        }
    }

    /** Encapsulated state object.  All data must be part of this object.  For
     * convenience, methods which operate on the data should also be part of
     * the object.
     */
    private volatile State state = null;


    /** Initialization method which must be called at least once.  If not
     * called explicitly, there will be a delay on playing the first sound.
     */
    public void init(Context context) {
        context = context.getApplicationContext();
        if (state == null) {
            synchronized (this) {
                if (state == null) {
                    // This code will only be executed once.  Multiple threads
                    // could pass the outer if test, but only one can be
                    // inside the synchronized block at a time.  The
                    // assignment of state is guaranteed to be visible to the
                    // other thread.
                    State nstate = new State(context);
                    state = nstate;
                }
            }
        }
        // Ideally, we would store the context and have an assert that we are
        // always passing the same application context, but then we run into
        // "ownership problems".  Since we are perfect, this will never happen
        // anyway.
    }

    /** Play a booming sound. */
    public void playBoom(Context context) {
        init(context);
        state.playSound(state.boom);
    }
}
