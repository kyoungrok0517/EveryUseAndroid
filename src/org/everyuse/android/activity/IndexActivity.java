package org.everyuse.android.activity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.everyuse.android.R;
import org.everyuse.android.model.User;
import org.everyuse.android.util.ErrorHelper;
import org.everyuse.android.util.URLHelper;
import org.everyuse.android.util.UserHelper;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class IndexActivity extends FragmentActivity {
	private EditText et_username;
	private EditText et_password;
	private String str_username;
	private String str_password;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_index);

		initUI();

		// TODO: 아이디와 패스워드를 저장하고, 시작 할때마다 로그인하는 방식으로 하자
		if (isAuthenticated()) {
			Intent intent = new Intent(IndexActivity.this, MainActivity.class);
			startActivity(intent);

			finish();
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.msg_register)
					.setCancelable(false)
					.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									Intent intent = new Intent(
											IndexActivity.this,
											RegisterActivity.class);
									startActivity(intent);
								}
							})
					.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});
			AlertDialog alert = builder.create();
			alert.setCancelable(true);
			alert.show();
		}
	}

	private boolean isAuthenticated() {
		return UserHelper.isAuthenticated(getApplicationContext());
	}

	private void initUI() {
		// initialize text fields
		et_username = (EditText) findViewById(R.id.et_username);
		et_password = (EditText) findViewById(R.id.et_password);

		
		// initialize buttons
		Button btn_login = (Button) findViewById(R.id.btn_login);
		btn_login.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// Get Username and Password String
				str_username = et_username.getText().toString().trim();
				str_password = et_password.getText().toString().trim();

				if (str_username.equals("") || str_password.equals("")) {
					Toast.makeText(getApplicationContext(),
							R.string.msg_complete_form, Toast.LENGTH_SHORT)
							.show();

					return;
				}

				new LoginTask().execute();
			}

		});

		Button btn_register = (Button) findViewById(R.id.btn_register);
		btn_register.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(IndexActivity.this,
						RegisterActivity.class);
				startActivity(intent);
			}

		});
	}

	private class LoginTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog indicator;
		private String error;

		@Override
		protected void onPreExecute() {
			// Initialize progress dialog
			indicator = new ProgressDialog(IndexActivity.this, ProgressDialog.STYLE_SPINNER);
			indicator.setMessage(getString(R.string.msg_wait));
			indicator.show();
		}

		@Override
		protected Boolean doInBackground(Void... args) {
			HttpClient client = new DefaultHttpClient();

			if (str_username.equals("") || str_password.equals("")) {
				return null;
			}

			// Make Parameters
			List<NameValuePair> params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("user_session[username]",
					str_username));
			params.add(new BasicNameValuePair("user_session[password]",
					str_password));

			try {
				HttpPost post = new HttpPost(URLHelper.USER_SESSIONS_URL
						+ ".json");
				HttpEntity req_entity = new UrlEncodedFormEntity(params,
						HTTP.UTF_8);
				post.setEntity(req_entity);

				HttpResponse response = client.execute(post);
				HttpEntity res_entity = response.getEntity();

				if (res_entity != null) {
					int code = response.getStatusLine().getStatusCode();
					try {
						String res_string = EntityUtils.toString(res_entity);

						if (code >= 300) { // error
							String[] fields = { "username", "password" };
							error = ErrorHelper.getMostProminentError(
									res_string, fields);

							return false;
						} else {
							JSONObject json = new JSONObject(res_string)
									.getJSONObject("record");
							User user = User.parseFromJSON(json);

							// store into shared preferences
							UserHelper.storeUser(getApplicationContext(), user);

							return true;
						}
					} catch (ParseException e1) {
						e1.printStackTrace();
					} catch (IOException e1) {
						e1.printStackTrace();
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Boolean result) {
			indicator.dismiss();

			if (result == null) {								// input form incomplete
				Toast.makeText(getApplicationContext(),
						getString(R.string.msg_complete_form),
						Toast.LENGTH_SHORT).show();
			} else if (result == false) {						// login failed
				Toast.makeText(getApplicationContext(), error,
						Toast.LENGTH_SHORT).show();
			} else if (result == true) {						// login success
				Toast.makeText(getApplicationContext(),
						getString(R.string.msg_login_success),
						Toast.LENGTH_SHORT).show();

				// move to the main
				Intent intent = new Intent(IndexActivity.this,
						MainActivity.class);
				startActivity(intent);

				// finish the activity
				finish();
			}
		}
	}
}