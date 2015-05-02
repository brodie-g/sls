/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.adam.aslfms.receiver;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.adam.aslfms.util.Track;
import com.adam.aslfms.util.Util;

import java.util.Iterator;
import java.util.Set;

/**
 * A BroadcastReceiver for intents sent by the Spotify Music Player.
 *
 * @see AbstractPlayStatusReceiver
 *
 * @author HumbleBeeBumbleBee HumbleBeeBumbleBeeDebugs@gmail.com
 * @since 1.4.8
 */

public class SpotifyReceiver extends AbstractPlayStatusReceiver {

	static final String APP_NAME = "Spotify";
	static final String TAG = "SpotifyReceiver";
	static final String TAG2 = "SpotDI";

	static final class BroadcastTypes {
		static final String APP_PACKAGE = "com.spotify.music";
		static final String PLAYBACK_STATE_CHANGED = APP_PACKAGE
				+ ".playbackstatechanged";
		static final String QUEUE_CHANGED = APP_PACKAGE + ".queuechanged";
		static final String METADATA_CHANGED = APP_PACKAGE + ".metadatachanged";
	}

	static private Track track = null;

	public static void dumpIntent(Bundle bundle) {
		if (bundle != null) {
			Set<String> keys = bundle.keySet();
			Iterator<String> it = keys.iterator();
			Log.e(TAG2, "Dumping Intent start");
			while (it.hasNext()) {
				String key = it.next();
				Log.e(TAG2, "[" + key + "=" + bundle.get(key) + "]");
			}
			Log.e(TAG2, "Dumping Intent end");
		}
	}

	/**
	 * Works well for Spotify premium users as I have tested the 7-day trial
	 * version.
	 *
	 * TODO: Make sure tracks are recognized after each commercial. Manual hack
	 * is to pause and play after each commercial. Not sure how to make a work
	 * around for free users... TODO: HELP with commercial hack!!!
	 *
	 * TODO: Spotify is buggy (at least on my phone). When UI is closed the
	 * music sometimes stops or continues, but Scrobbling usually stops until
	 * Spotify process is ended and restarted.
	 *
	 * TODO: As @metanota has explained, repeating one song does not Scrobble
	 * after first scrobble for SpotifyReceiver.
	 * https://github.com/tgwizard/sls/pull/149
	 * 
	 */
	@Override
	protected void parseIntent(Context ctx, String action, Bundle bundle) {

		MusicAPI musicAPI = MusicAPI.fromReceiver(ctx, APP_NAME,
				BroadcastTypes.APP_PACKAGE, null, false);
		setMusicAPI(musicAPI);

		// This is sent with all broadcasts, regardless of type. The value is
		// taken from System.currentTimeMillis(), which you can compare to in
		// order to determine how old the event is.
		long timeSentInMs = bundle.getLong("timeSent", 0L);
		Log.d(TAG, "Time Sent In Millis: " + timeSentInMs);

		if (action.equals(BroadcastTypes.PLAYBACK_STATE_CHANGED)) {
			// Called usually only when user presses play or pause
			// Sometimes calls RESUME at start of song or during play, possibly
			// due to minor queue or play-back errors respectively.
			boolean playing = bundle.getBoolean("playing", false);
			int positionInMs = bundle.getInt("playbackPosition", 0);
			// ^positionInMs is needed?
			Log.d(TAG, "Position in Millis: " + positionInMs
					+ " Position in Secs: " + (positionInMs / 1000));
			if (playing) {
				setState(Track.State.RESUME);
				Log.d(TAG, "Setting state to RESUME");
			} else if (!playing) {
				setState(Track.State.PAUSE);
				Log.d(TAG, "Setting state to PAUSE");
			}
		/**} else if (action.equals(BroadcastTypes.QUEUE_CHANGED)) {
			// Calls QUEUE once if tracks are played freely. Sends immediately
			// after song begins play.
			setState(Track.State.COMPLETE);
			Log.d(TAG2, "Setting state to COMPLETE in QUEUE_CHANGED");
			setState(Track.State.START);
			Log.d(TAG2, "Setting state to START in QUEUE_CHANGED");
		*/
		} else if (action.equals(BroadcastTypes.METADATA_CHANGED)) {
			// Calls METADATA twice (if no pause) once before and once after
			// QUEUE_CHANGED. 4 calls per pause-play
			if (bundle.getString("id").contains(":ad:")) {
				Log.e(TAG2, "Identified ad " + bundle.getString("id") + " ad length " + bundle.getInt("length", 0));
			} else {
				setState(Track.State.CHANGED);
				Track.Builder b = new Track.Builder();
				b.setMusicAPI(musicAPI);
				b.setWhen(Util.currentTimeSecsUTC());

				b.setArtist(bundle.getString("artist"));
				b.setAlbum(bundle.getString("album"));
				b.setTrack(bundle.getString("track"));
				b.setDuration(bundle.getInt("length", 0));
				Log.d(TAG,
						bundle.getString("artist") + " - "
								+ bundle.getString("track") + " ("
								+ bundle.getInt("length", 0) + ")");
				setTrack(b.build());
			}
		}
		dumpIntent(bundle);
	}
}
