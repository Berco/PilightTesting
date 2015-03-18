/*
 * ******************************************************************************
 *   Copyright (c) 2013 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package by.zatta.pilight.views;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import by.zatta.pilight.R;
import it.gmariotti.cardslib.library.internal.CardHeader;

public class CustomHeaderInnerCard extends CardHeader {

	String device;
	String location;

	public CustomHeaderInnerCard(Context context, String deviceName, String locationName) {
		super(context, R.layout.header_inner);
		device = deviceName;
		location = locationName;
	}

	@Override
	public void setupInnerViewElements(ViewGroup parent, View view) {
		if (view != null) {
			TextView t1 = (TextView) view.findViewById(R.id.tvHeaderDevice);
			if (t1 != null)
				t1.setText(device);

			TextView t2 = (TextView) view.findViewById(R.id.tvHeaderLocation);
			if (t2 != null)
				t2.setText(location);
		}
	}
}
