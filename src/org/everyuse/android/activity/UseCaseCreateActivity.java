package org.everyuse.android.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.everyuse.android.R;
import org.everyuse.android.model.UseCase;
import org.everyuse.android.model.User;
import org.everyuse.android.util.ErrorHelper;
import org.everyuse.android.util.MediaHelper;
import org.everyuse.android.util.URLHelper;
import org.everyuse.android.util.UserHelper;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class UseCaseCreateActivity extends SherlockActivity {
	private EditText et_item;
	private EditText et_purpose;
	private Spinner sp_purpose_type;
	private Spinner sp_place;
	private ImageButton btn_photo_select;
	private ImageView iv_photo;

	// photo
	private String camera_photo_uri_temp;
	private Uri camera_photo_uri;
	private File photo_file;
	private static final int PICK_FROM_CAMERA = 0;
	private static final int PICK_FROM_ALBUM = 1;

	// input
	private String input_item;
	private String input_purpose;
	private String input_purpose_type;
	private String input_place;
	private File input_photo_file;

	// EXTRAs
	public static final String EXTRA_ITEM = "item";
	public static final String EXTRA_PURPOSE = "purpose";
	private String pre_item;
	private String pre_purpose;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_create);
		setTitle(R.string.title_activity_create);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		// UI 초기화
		initUI();

		// handle intent
		handleIntent(getIntent());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.create, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.menu_create:
			// 서버로 데이터 전송
			new SubmitTask().execute();
			break;
		case android.R.id.home:
			Intent intent = new Intent(this, MainActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			startActivity(intent);
		}

		return true;
	}

	private void handleIntent(Intent intent) {
		if (intent != null) {
			pre_item = intent.getStringExtra(EXTRA_ITEM);
			pre_purpose = intent.getStringExtra(EXTRA_PURPOSE);

			if (pre_item != null) {
				et_item.setText(pre_item);
			}

			if (pre_purpose != null) {
				et_purpose.setText(pre_purpose);
			}
		}
	}

	private void initUI() {
		et_item = (EditText) findViewById(R.id.et_item);
		et_purpose = (EditText) findViewById(R.id.et_purpose);

		// purpose type Spinner 초기화
		sp_purpose_type = (Spinner) findViewById(R.id.sp_purpose_type);
		ArrayAdapter<CharSequence> purpose_type_adapter = ArrayAdapter
				.createFromResource(this, R.array.purpose,
						android.R.layout.simple_spinner_item);
		purpose_type_adapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		sp_purpose_type.setAdapter(purpose_type_adapter);

		// 장소 Spinner 초기화
		sp_place = (Spinner) findViewById(R.id.sp_place);
		int place_array_id = R.array.place_student;		// TODO 장소 어레이는 일단 학생용으로...

		if (place_array_id != 0) {
			ArrayAdapter<CharSequence> place_adapter = ArrayAdapter
					.createFromResource(this, place_array_id,
							android.R.layout.simple_spinner_item);
			place_adapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			sp_place.setAdapter(place_adapter);
		}

		// 사진 ImageView 초기화
		iv_photo = (ImageView) findViewById(R.id.iv_photo);

		// 여기서 기본 스타일로 지정된 디폴트 이미지를 지움
		iv_photo.setImageDrawable(null);
		iv_photo.setBackgroundResource(android.R.color.background_dark);

		btn_photo_select = (ImageButton) findViewById(R.id.btn_photo_select);
		btn_photo_select.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				DialogInterface.OnClickListener cameraListener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						doTakePhotoAction();
					}
				};

				DialogInterface.OnClickListener albumListener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						doTakeAlbumAction();
					}
				};

				DialogInterface.OnClickListener cancelListener = new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}
				};

				new AlertDialog.Builder(UseCaseCreateActivity.this)
						.setTitle("Select the method")
						.setPositiveButton(getString(R.string.btn_from_camera),
								cameraListener)
						.setNeutralButton(getString(R.string.btn_from_gallery),
								albumListener)
						.setNegativeButton("Cancel", cancelListener).show();
			}

		});

		Button btn_submit = (Button) findViewById(R.id.btn_submit);
		btn_submit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// 서버로 데이터 전송
				new SubmitTask().execute();
			}

		});
	}

	/**
	 * 카메라에서 이미지 가져오기
	 */
	private void doTakePhotoAction() {
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		// 임시로 사용할 파일의 경로를 생성
		camera_photo_uri = MediaHelper.getOutputMediaFileUri(MediaHelper.MEDIA_TYPE_IMAGE);
		camera_photo_uri_temp = camera_photo_uri.toString();
		intent.putExtra(MediaStore.EXTRA_OUTPUT, camera_photo_uri);
		startActivityForResult(intent, PICK_FROM_CAMERA);
	}

	/**
	 * 앨범에서 이미지 가져오기
	 */
	private void doTakeAlbumAction() {
		// 앨범 호출
		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("image/*");
		startActivityForResult(Intent.createChooser(intent, "Select Picture"),
				PICK_FROM_ALBUM);
	}

	private class SubmitTask extends AsyncTask<Void, Void, Boolean> {
		private ProgressDialog indicator;
		private String msg_error;

		private UseCase created;

		@Override
		protected void onPreExecute() {
			indicator = new ProgressDialog(UseCaseCreateActivity.this,
					ProgressDialog.STYLE_SPINNER);
			indicator.setMessage("Please wait...");
			indicator.show();

			// 입력된 'item'
			input_item = et_item.getText().toString();

			// 입력된 'purpose'
			input_purpose = et_purpose.getText().toString();

			// purpose type이 선택되지 않았다면, 그냥 빈 String으로 입력
			Object purpose_type_selected = sp_purpose_type.getSelectedItem();
			input_purpose_type = (purpose_type_selected == null) ? ""
					: purpose_type_selected.toString().toLowerCase();

			// place가 선택되지 않았다면, 그냥 빈 String으로 입력
			Object place_selected = sp_place.getSelectedItem();
			input_place = (place_selected == null) ? "" : sp_place
					.getSelectedItem().toString().toLowerCase();

			// 선택된 사진
			input_photo_file = photo_file;

		}

		@Override
		protected Boolean doInBackground(Void... args) {
			String url = URLHelper.USE_CASES_RECENT_URL + ".json";

			HttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);

			MultipartEntity entity = new MultipartEntity(
					HttpMultipartMode.BROWSER_COMPATIBLE);
			try {
				User user = UserHelper.getCurrentUser(getApplicationContext());

				entity.addPart("use_case[item]", new StringBody(input_item,
						Charset.forName("UTF-8")));
				entity.addPart("use_case[purpose]", new StringBody(
						input_purpose, Charset.forName("UTF-8")));
				entity.addPart("use_case[purpose_type]", new StringBody(
						input_purpose_type, Charset.forName("UTF-8")));
				entity.addPart("use_case[place]", new StringBody(input_place,
						Charset.forName("UTF-8")));
				entity.addPart("use_case[photo]", new FileBody(
						input_photo_file, "image/png"));
				entity.addPart("user_credentials", new StringBody(
						user.single_access_token, Charset.forName("UTF-8")));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return false;
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			httpPost.setEntity(entity);

			try {
				HttpResponse response = httpClient.execute(httpPost);
				HttpEntity resEntity = response.getEntity();
				String responseString = EntityUtils.toString(resEntity);
				int statusCode = response.getStatusLine().getStatusCode();

				if (statusCode >= 300) { // error occurred
					String[] fields = { "item", "purpose", "photo" };
					try {
						msg_error = ErrorHelper.getMostProminentError(
								responseString, fields);
					} catch (JSONException e) {
						Log.d("PostActivity", responseString);
					}

					return false;
				} else {
					JSONObject json = null;
					try {
						json = new JSONObject(responseString);
						created = UseCase.parseSingleFromJSON(json);
					} catch (JSONException e) {
						e.printStackTrace();
						return false;
					}

					return true;
				}
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			return false;
		}

		@Override
		protected void onPostExecute(Boolean success) {
			indicator.dismiss();

			if (success) {
				Toast.makeText(UseCaseCreateActivity.this,
						R.string.msg_create_success, Toast.LENGTH_SHORT).show();

				Intent intent = new Intent(UseCaseCreateActivity.this,
						UseCaseDetailActivity.class);
				intent.putExtra(UseCaseDetailActivity.EXTRA_DATA, created);
				startActivity(intent);

				finish();
			} else {
				Toast.makeText(UseCaseCreateActivity.this, msg_error,
						Toast.LENGTH_SHORT).show();
			}
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case PICK_FROM_ALBUM: {
				Uri photo_uri = data.getData();
				InputStream is = null;

				try {
					is = getContentResolver().openInputStream(photo_uri);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}

				// Resize and rotate the original bitmap
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 3;
				Bitmap resized = BitmapFactory.decodeStream(is, null, options);

				new SaveBitmapToSD().execute(resized);
				break;
			}

			case PICK_FROM_CAMERA: {
				String photo_path = getPath(Uri.parse(camera_photo_uri_temp));

				// Resize and rotate the original bitmap
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inSampleSize = 3;
				Bitmap bitmap = rotateBitmap(
						BitmapFactory.decodeFile(photo_path, options), 90);

				new SaveBitmapToSD().execute(bitmap);
				break;
			}
			}
		}
	}

	private Bitmap rotateBitmap(Bitmap original, int degree) {
		Matrix mat = new Matrix();
		mat.postRotate(degree);
		Bitmap rotated = Bitmap.createBitmap(original, 0, 0,
				original.getWidth(), original.getHeight(), mat, true);

		return rotated;
	}

	private String getPath(Uri uri) {
		String selectedImagePath;
		// 1:MEDIA GALLERY --- query from MediaStore.Images.Media.DATA
		String[] projection = { MediaStore.Images.Media.DATA };
		Cursor cursor = getContentResolver().query(uri, projection, null, null,
				null);
		if (cursor != null) {
			int column_index = cursor
					.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
			cursor.moveToFirst();
			selectedImagePath = cursor.getString(column_index);
		} else {
			selectedImagePath = uri.getPath();
		}

		return selectedImagePath;
	}

	private class SaveBitmapToSD extends AsyncTask<Bitmap, Void, Void> {
		private ProgressDialog indicator;
		private File bitmap_file;
		private Bitmap bitmap;

		@Override
		protected void onPreExecute() {
			indicator = new ProgressDialog(UseCaseCreateActivity.this,
					ProgressDialog.STYLE_SPINNER);
			indicator.setMessage("Processing...");
			indicator.show();
		}

		@Override
		protected Void doInBackground(Bitmap... photo) {
			bitmap = photo[0];

			if (bitmap != null) {
				try {
					bitmap_file = File.createTempFile("upload_", ".jpg",
							getExternalCacheDir());
				} catch (IOException e) {
					e.printStackTrace();
				}

				try {
					FileOutputStream out = new FileOutputStream(bitmap_file);
					bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			indicator.dismiss();

			// 업로드할 파일 set
			if (bitmap_file != null) {
				photo_file = bitmap_file;

				// set preview
				iv_photo.setImageBitmap(bitmap);
			} else {
				Toast.makeText(getApplicationContext(), "Cannot process photo",
						Toast.LENGTH_SHORT).show();
			}
		}

	}
}