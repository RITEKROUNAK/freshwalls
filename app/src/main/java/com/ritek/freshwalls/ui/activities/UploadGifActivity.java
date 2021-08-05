package com.ritek.freshwalls.ui.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import androidx.annotation.RequiresApi;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.ritek.freshwalls.adapter.CategorySelectAdapter;
import com.ritek.freshwalls.adapter.ColorSelectAdapter;
import com.ritek.freshwalls.adapter.SelectableCategoryViewHolder;
import com.ritek.freshwalls.adapter.SelectableColorViewHolder;
import com.ritek.freshwalls.manager.PrefManager;
import com.ritek.freshwalls.R;
import com.ritek.freshwalls.api.ProgressRequestBody;
import com.ritek.freshwalls.api.apiClient;
import com.ritek.freshwalls.api.apiRest;
import com.ritek.freshwalls.entity.ApiResponse;
import com.ritek.freshwalls.entity.Category;
import com.ritek.freshwalls.entity.Color;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import es.dmoral.toasty.Toasty;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class UploadGifActivity extends AppCompatActivity implements ProgressRequestBody.UploadCallbacks,SelectableCategoryViewHolder.OnItemSelectedListener ,SelectableColorViewHolder.OnItemSelectedListener {

    private Spinner spinner_categories_upload;
    private ArrayList<CharSequence> categoriesList = new ArrayList<>();
    private RelativeLayout relative_layout_upload;
    private ArrayAdapter<CharSequence> adapter;



    private RecyclerView recycle_view_selected_color;
    private RecyclerView recycle_view_selected_category;


    private LinearLayoutManager gridLayoutManagerCategorySelect;
    private LinearLayoutManager gridLayoutManagerColorSelect;

    private ArrayList<Category> categoriesListObj = new ArrayList<Category>();
    private CategorySelectAdapter categorySelectAdapter;
    private ColorSelectAdapter colorSelectAdapter;

    private List<Color> colorList = new ArrayList<Color>();

    protected Button selectColoursButton;

    protected String[] colours ;

    protected ArrayList<CharSequence> selectedColours = new ArrayList<CharSequence>();
    private LinearLayoutManager linearLayoutManager_color;
    private RecyclerView recycle_view_colors_fragment;
    private int PICK_IMAGE = 1002;
    private Bitmap bitmap_wallpaper;
    private ProgressDialog register_progress;

    private EditText edit_text_upload_title;
    private static final int CAMERA_REQUEST_IMAGE_1 = 3001;
    private String   videoUrl;
    private ProgressDialog pd;
    private LinearLayout linear_layout_select;
    private FloatingActionButton fab_upload;
    private EditText edit_text_upload_description;
    private LinearLayout linear_layout_lang;
    private LinearLayout linear_layout_categories;

    public void checkPermission(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (ContextCompat.checkSelfPermission(UploadGifActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)  != PackageManager.PERMISSION_GRANTED) {


                if (ActivityCompat.shouldShowRequestPermissionRationale(UploadGifActivity.this,   Manifest.permission.READ_CONTACTS)) {
                    Intent intent_status  =  new Intent(getApplicationContext(), PermissionActivity.class);
                    startActivity(intent_status);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    finish();
                } else {
                    Intent intent_status  =  new Intent(getApplicationContext(), PermissionActivity.class);
                    startActivity(intent_status);
                    overridePendingTransition(R.anim.enter, R.anim.exit);
                    finish();
                }
            }

        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        checkPermission();
        setContentView(R.layout.activity_upload_gif);
        loadLang();
        initView();
        initAction();

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.upload_wallpaper_gif));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    private void initAction() {
        linear_layout_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SelectImage();
            }
        });

        fab_upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edit_text_upload_title.getText().toString().trim().length()<3){
                    Toasty.error(UploadGifActivity.this, getResources().getString(R.string.edit_text_upload_title_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                if (videoUrl==null){
                    Toasty.error(UploadGifActivity.this, getResources().getString(R.string.image_upload_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                upload(CAMERA_REQUEST_IMAGE_1);
            }
        });

    }

    private void SelectImage() {
        if (ContextCompat.checkSelfPermission(UploadGifActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UploadGifActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {

            boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
            if (isKitKat)
            {
                Intent uploadIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                uploadIntent.setType("image/gif");
                uploadIntent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(uploadIntent, PICK_IMAGE);

            }
            else
            {
                Intent uploadIntent = new Intent(Intent.ACTION_GET_CONTENT);
                uploadIntent.setType("image/gif");
                startActivityForResult(uploadIntent, PICK_IMAGE);
            }


        }
    }

    private void initView() {
        pd = new ProgressDialog(UploadGifActivity.this);
        pd.setMessage("Uploading GIF");
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setCancelable(false);
        this.linear_layout_categories =  (LinearLayout) findViewById(R.id.linear_layout_categories);
        this.linear_layout_lang =  (LinearLayout) findViewById(R.id.linear_layout_lang);
        this.edit_text_upload_description =  (EditText) findViewById(R.id.edit_text_upload_description);
        this.fab_upload =  (FloatingActionButton) findViewById(R.id.fab_upload);
        this.linear_layout_select =  (LinearLayout) findViewById(R.id.linear_layout_select);
        this.edit_text_upload_title=(EditText) findViewById(R.id.edit_text_upload_title);
        this.relative_layout_upload=(RelativeLayout) findViewById(R.id.relative_layout_upload);

        PrefManager prf= new PrefManager(getApplicationContext());

        this.linearLayoutManager_color = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);



        gridLayoutManagerCategorySelect = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false);
        gridLayoutManagerColorSelect = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false);

        recycle_view_selected_category= (RecyclerView) findViewById(R.id.recycle_view_selected_category);
        recycle_view_selected_color= (RecyclerView) findViewById(R.id.recycle_view_selected_color);
        getCategory();
    }

    protected void showSelectColoursDialog() {

        boolean[] checkedColours = new boolean[colours.length];
        int count = colours.length;
        for(int i = 0; i < count; i++)
            checkedColours[i] = selectedColours.contains(colours[i]);
        DialogInterface.OnMultiChoiceClickListener coloursDialogListener = new DialogInterface.OnMultiChoiceClickListener() {

            @Override

            public void onClick(DialogInterface dialog, int which, boolean isChecked) {

                if(isChecked)
                    selectedColours.add(colours[which]);
                else
                    selectedColours.remove(colours[which]);


            }

        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Select Colours");
        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });




        builder.setMultiChoiceItems(colours, checkedColours, coloursDialogListener);
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        // check here to KITKAT or new version
        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {

            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    public static String getDataColumn(Context context, Uri uri,
                                       String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

    /**
     * @param uri
     *            The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri
                .getAuthority());
    }
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK
                && null != data) {
            if (resultCode == RESULT_OK) {


                if (data == null) {
                    //no data present
                    return;
                }


                Uri selectedFileUri = data.getData();
                videoUrl = getPath(this, selectedFileUri);
                String selectedFilePath = getPath(this, selectedFileUri);
                Log.i("UPLOAD", "Selected File Path:" + selectedFilePath);


                File file = new File(selectedFilePath);
                edit_text_upload_title.setText(file.getName().replace(".gif","").replace("GIF",""));



                if (selectedFilePath != null && !selectedFilePath.equals("")) {
                    Log.i("UPLOAD", "Selected File Path:" + selectedFilePath);
                } else {
                    Toast.makeText(this, "Cannot upload file to server", Toast.LENGTH_SHORT).show();
                }
            }

            /*
            Uri selectedGif = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedGif,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();

            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            bitmap_wallpaper = BitmapFactory.decodeFile(picturePath);


  ;*/


        } else {

            Log.i("SonaSys", "resultCode: " + resultCode);
            switch (resultCode) {
                case 0:
                    Log.i("SonaSys", "User cancelled");
                    break;
                case -1:
                    break;
            }
        }
    }
    public void upload(final int CODE){
        File file1 = new File(videoUrl);
        int file_size = Integer.parseInt(String.valueOf(file1.length()/1024/1024));
        if (file_size>20){
            Toasty.error(getApplicationContext(),"Max file size allowed 20M",Toast.LENGTH_LONG).show();
        }
        Log.v("SIZE",file1.getName()+"");


        pd.show();
        PrefManager prf = new PrefManager(getApplicationContext());

        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);

        //File creating from selected URL
        final File file = new File(videoUrl);


        ProgressRequestBody requestFile = new ProgressRequestBody(file, UploadGifActivity.this);

        // create RequestBody instance from file
        // RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);

        // MultipartBody.Part is used to send also the actual file name
        MultipartBody.Part body =MultipartBody.Part.createFormData("uploaded_file", file.getName(), requestFile);
        String id_ser=  prf.getString("ID_USER");
        String key_ser=  prf.getString("TOKEN_USER");

        Call<ApiResponse> request = service.uploadGif(body, id_ser, key_ser, edit_text_upload_title.getText().toString().trim(),edit_text_upload_description.getText().toString().trim(),getSelectedColors(),getSelectedCategories());

        request.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {

                if (response.isSuccessful()){
                    Toasty.success(getApplication(),getResources().getString(R.string.gif_upload_success),Toast.LENGTH_LONG).show();
                    finish();
                }else{
                    Toasty.error(getApplication(),getResources().getString(R.string.no_connexion),Toast.LENGTH_LONG).show();

                }
                // file.delete();
                // getApplicationContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
                pd.dismiss();
                pd.cancel();
            }
            @Override
            public void onFailure(Call<ApiResponse> call, Throwable t) {
                Toasty.error(getApplication(),getResources().getString(R.string.no_connexion),Toast.LENGTH_LONG).show();
                pd.dismiss();
                pd.cancel();
            }
        });
    }
    public String getSelectedCategories(){
        String categories = "";
        for (int i = 0; i < categorySelectAdapter.getSelectedItems().size(); i++) {
            categories+="_"+categorySelectAdapter.getSelectedItems().get(i).getId();
        }
        Log.v("categories",categories);

        return categories;
    }
    public String getSelectedColors(){
        String colors = "";
        for (int i = 0; i < colorSelectAdapter.getSelectedItems().size(); i++) {
            colors+="_"+colorSelectAdapter.getSelectedItems().get(i).getId();
        }
        Log.v("colors",colors);
        return colors;
    }
    @Override
    public void onProgressUpdate(int percentage) {
        pd.setProgress(percentage);
    }

    @Override
    public void onError() {
        pd.dismiss();
        pd.cancel();
    }

    @Override
    public void onFinish() {
        pd.dismiss();
        pd.cancel();

    }
    public String getRealPathFromURI(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
        return cursor.getString(idx);
    }
    @Override
    public void onBackPressed(){
        super.onBackPressed();
        // overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
        return;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                //  overridePendingTransition(R.anim.slide_enter, R.anim.slide_exit);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void loadLang(){
        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<Color>> call = service.ColorsList();
        call.enqueue(new Callback<List<Color>>() {
            @Override
            public void onResponse(Call<List<Color>> call, final Response<List<Color>> response) {
                if (response.isSuccessful()){
                    if (response.body().size()>1){
                        linear_layout_lang.setVisibility(View.VISIBLE);
                    }
                }
            }
            @Override
            public void onFailure(Call<List<Color>> call, Throwable t) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission();
    }


    private void getCategory() {
        register_progress= ProgressDialog.show(this, null,getResources().getString(R.string.operation_progress), true);

        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<Category>> call = service.categoryAll();
        call.enqueue(new Callback<List<Category>>() {
            @Override
            public void onResponse(Call<List<Category>> call, Response<List<Category>> response) {
                if(response.isSuccessful()){
                        categoriesListObj.clear();
                        for (int i = 0; i < response.body().size(); i++) {

                            categoriesListObj.add(response.body().get(i));
                        }
                        categorySelectAdapter = new CategorySelectAdapter(UploadGifActivity.this, categoriesListObj, true, UploadGifActivity.this);
                        recycle_view_selected_category.setHasFixedSize(true);
                        recycle_view_selected_category.setAdapter(categorySelectAdapter);
                        recycle_view_selected_category.setLayoutManager(gridLayoutManagerCategorySelect);
                    if (response.body().size()>0) {
                        linear_layout_categories.setVisibility(View.VISIBLE);
                    }
                }else {
                    register_progress.dismiss();
                    Snackbar snackbar = Snackbar
                            .make(relative_layout_upload, getResources().getString(R.string.no_connexion), Snackbar.LENGTH_INDEFINITE)
                            .setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    getCategory();
                                }
                            });
                    snackbar.show();
                }
                getColors();
            }
            @Override
            public void onFailure(Call<List<Category>> call, Throwable t) {
                register_progress.dismiss();
                Snackbar snackbar = Snackbar
                        .make(relative_layout_upload, getResources().getString(R.string.no_connexion), Snackbar.LENGTH_INDEFINITE)
                        .setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getCategory();
                            }
                        });
                snackbar.setActionTextColor(android.graphics.Color.RED);
                snackbar.show();
                getColors();

            }
        });
    }
    private void getColors(){
        Retrofit retrofit = apiClient.getClient();
        apiRest service = retrofit.create(apiRest.class);
        Call<List<Color>> call = service.ColorsList();
        call.enqueue(new Callback<List<Color>>() {
            @Override
            public void onResponse(Call<List<Color>> call, Response<List<Color>> response) {
                if(response.isSuccessful()) {
                    List<String> colortitles = new ArrayList<String>();
                    colorList.clear();
                    for (int i = 0; i < response.body().size(); i++) {
                        if (i!=0){
                            colorList.add(response.body().get(i));
                            colortitles.add(response.body().get(i).getTitle());
                        }
                    }

                    register_progress.dismiss();
                    colorSelectAdapter = new ColorSelectAdapter(UploadGifActivity.this, colorList, true, UploadGifActivity.this);
                    recycle_view_selected_color.setHasFixedSize(true);
                    recycle_view_selected_color.setAdapter(colorSelectAdapter);
                    recycle_view_selected_color.setLayoutManager(gridLayoutManagerColorSelect);
                    if (response.body().size()>1){
                        linear_layout_lang.setVisibility(View.VISIBLE);
                    }

                    //fab_save_upload.show();
                }else{
                    register_progress.dismiss();
                    Snackbar snackbar = Snackbar
                            .make(relative_layout_upload, getResources().getString(R.string.no_connexion), Snackbar.LENGTH_INDEFINITE)
                            .setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    getColors();
                                }
                            });
                    snackbar.setActionTextColor(android.graphics.Color.RED);
                    snackbar.show();
                }

            }
            @Override
            public void onFailure(Call<List<Color>> call, Throwable t) {
                register_progress.dismiss();
                Snackbar snackbar = Snackbar
                        .make(relative_layout_upload, getResources().getString(R.string.no_connexion), Snackbar.LENGTH_INDEFINITE)
                        .setAction(getResources().getString(R.string.retry), new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getColors();
                            }
                        });
                snackbar.setActionTextColor(android.graphics.Color.RED);
                snackbar.show();
            }
        });
    }

    @Override
    public void onItemSelected(Color item) {

    }

    @Override
    public void onItemSelected(Category item) {

    }
}
