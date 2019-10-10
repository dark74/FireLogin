package com.dk.firelogin;

//import android.support.v7.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dk.firelogin.bean.User;
import com.dk.firelogin.sp.GlobalSP;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    // Choose an arbitrary request code value
    private static final int RC_SIGN_IN = 123;
    private TextView tv_title;
    private Button btn_login;
    private Button btn_upload;
    private Button btn_download;
    // cloud storage
    private FirebaseStorage storage;
    private ImageView img_cloud;
    private ProgressBar progress_upload;
    private Button btn_local_upload;
    private FirebaseUser user;
    private Button btn_download_local;
    private Button btn_download_firebase_ui;
    private static final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private static final int permissionRequestCode = 20000;
    private static boolean isPermissionGranted = false;
    private Button btn_list_all;
    private ListView list;
    private Button btn_real_time_db;
    private Button btn_real_time_db_read;
    private Button btn_real_time_db_write;
    // firebase db
    private FirebaseDatabase database;
    List<User> userList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initDb();
        requestPermission();
        initCloudStorage();
    }

    private void initDb() {
        database = FirebaseDatabase.getInstance();
        userList = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            User user = new User("jack1", (1003+i), 1);
            userList.add(user);
        }
    }

    private void requestPermission() {
        for (int i = 0; i < permissions.length; i++) {
            if (ContextCompat.checkSelfPermission(MainActivity.this, permissions[i]) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{permissions[i]}, permissionRequestCode);
                }
            } else {
                isPermissionGranted = true;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case permissionRequestCode:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // success
                    isPermissionGranted = true;
                } else {
                    Toast.makeText(MainActivity.this, "需要授权才可以下载文件", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private void initCloudStorage() {
        storage = FirebaseStorage.getInstance();
        Log.i(TAG, "存储storage:" + storage.toString());
        StorageReference storageReference = storage.getReference();
        StorageReference imagesRef = storageReference.child("images");
        if (imagesRef != null) {
            Log.i(TAG, "images文件夹：" + imagesRef.getPath());
        }
        StorageReference scan1Ref = storageReference.child("images/scan_1.png");
        scan1Ref.getPath();
        if (scan1Ref != null) {
            Log.i(TAG, "images文件夹：" + scan1Ref.getName());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG, "响应requestCode：" + requestCode + ", resultCode:" + resultCode);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                Log.i(TAG, "登录成功");
                // Successfully signed in
                user = FirebaseAuth.getInstance().getCurrentUser();
                GlobalSP.getInstance(MainActivity.this);
                // ...
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private void initView() {
        tv_title = (TextView) findViewById(R.id.tv_title);
        btn_login = (Button) findViewById(R.id.btn_login);
        btn_login.setOnClickListener(this);
        btn_upload = (Button) findViewById(R.id.btn_upload);
        btn_upload.setOnClickListener(this);
        btn_download = (Button) findViewById(R.id.btn_download);
        btn_download.setOnClickListener(this);
        img_cloud = (ImageView) findViewById(R.id.img_cloud);
        img_cloud.setOnClickListener(this);
        progress_upload = (ProgressBar) findViewById(R.id.progress_upload);
        progress_upload.setOnClickListener(this);
        btn_local_upload = (Button) findViewById(R.id.btn_local_upload);
        btn_local_upload.setOnClickListener(this);
        btn_download_local = (Button) findViewById(R.id.btn_download_local);
        btn_download_local.setOnClickListener(this);
        btn_download_firebase_ui = (Button) findViewById(R.id.btn_download_firebase_ui);
        btn_download_firebase_ui.setOnClickListener(this);
        btn_list_all = (Button) findViewById(R.id.btn_list_all);
        btn_list_all.setOnClickListener(this);
        list = (ListView) findViewById(R.id.list);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "点击第" + (position + 1) + "个");
            }
        });
        btn_real_time_db = (Button) findViewById(R.id.btn_real_time_db_read);
        btn_real_time_db.setOnClickListener(this);
        btn_real_time_db_read = (Button) findViewById(R.id.btn_real_time_db_read);
        btn_real_time_db_read.setOnClickListener(this);
        btn_real_time_db_write = (Button) findViewById(R.id.btn_real_time_db_write);
        btn_real_time_db_write.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_login:
                login();
                break;
            case R.id.btn_upload:
                uploadFile();
                break;
            case R.id.btn_download:
                download();
                break;
            case R.id.btn_local_upload:
                localUpload();
                break;
            case R.id.btn_download_local:
                break;
            case R.id.btn_download_firebase_ui:
                break;
            case R.id.btn_list_all:
                listAll();
                break;
            case R.id.btn_real_time_db_read:
                testRTDB_read();
                break;
            case R.id.btn_real_time_db_write:
                testRTDB_write();
                break;
        }
    }

    private void testRTDB_read() {
        if (database == null) {
            Toast.makeText(MainActivity.this, "database未初始化", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "database未初始化" );
            return;
        }
        if (user == null) {
            Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        DatabaseReference rootRef = database.getReference();
        rootRef.child("users").child(user.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.i(TAG, "数据库变动- key:"+ dataSnapshot.getKey() + "value:"+dataSnapshot.getValue());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "messages:onCancelled:" + databaseError.getMessage());
            }
        });
        //Query
        Query baseQuery = rootRef.child("users").child(user.getUid()).orderByKey();
        baseQuery.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.i(TAG, "数据库查询- key:"+ dataSnapshot.getKey() + "value:"+dataSnapshot.getValue());

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.i(TAG, "数据库查询- key:"+ dataSnapshot.getKey() + "value:"+dataSnapshot.getValue());

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.i(TAG, "数据库查询- key:"+ dataSnapshot.getKey() + "value:"+dataSnapshot.getValue());

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.i(TAG, "数据库查询- key:"+ dataSnapshot.getKey() + "value:"+dataSnapshot.getValue());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i(TAG, "数据库查询- msg:"+ databaseError.getMessage() + ",code:"+databaseError.getCode());
            }
        });

    }

    private void testRTDB_write() {
        if (database == null) {
            Toast.makeText(MainActivity.this, "database未初始化", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "database未初始化" );
            return;
        }
        if (user == null) {
            Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        DatabaseReference rootRef = database.getReference();

        User testUser = new User("tom", 1001, 0);
        User testUser2 = new User("perks", 1002, 1);
        userList.add(testUser);
        userList.add(testUser2);

        rootRef.child("users").child(user.getUid()).setValue(userList)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "写入数据库失败", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "写入数据库失败，E:"+ e.getMessage());
                    }
                })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "写入数据库成功", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "写入数据库成功");
                    }
                });

    }

    private void listAll() {
        StorageReference storageReference = storage.getReference();
        if (user == null) {
            Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        StorageReference rootRef = storageReference.child("user/" + user.getUid());
        Task<ListResult> listResultTask = rootRef.listAll();
        listResultTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "列表失败，请重试：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "列表失败，请重试：" + e.getMessage());
            }
        });
        listResultTask.addOnSuccessListener(new OnSuccessListener<ListResult>() {
            @Override
            public void onSuccess(ListResult listResult) {
                Toast.makeText(MainActivity.this, "列表成功", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "列表成功,list:" + listResult.getItems().size());
                list.setAdapter(new MyListAdapter(listResult.getItems()));
            }
        });
    }

    private void download() {
        if (!isPermissionGranted) {
            Toast.makeText(MainActivity.this, "未授予存储权限", Toast.LENGTH_SHORT).show();
            return;
        }
        if (user == null) {
            Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        // Create a storage reference from our app
        StorageReference storageRef = storage.getReference();

        StorageReference downLoadImageRef = storageRef.child("user/" + user.getUid() + "/images/scan_1.png");

        try {
            File scanImg = new File(Environment.getExternalStorageDirectory(), System.currentTimeMillis() + ".png");
            downLoadImageRef.getFile(scanImg).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(MainActivity.this, "下载成功", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "下载成功");
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(MainActivity.this, "下载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "下载失败：" + e.getMessage());
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        //downLoadImageRef.getDownloadUrl();//获取下载地址
        // Create a reference with an initial file path and name
        //StorageReference pathReference = storageRef.child("images/stars.jpg");

        // Create a reference to a file from a Google Cloud Storage URI
        //StorageReference gsReference = storage.getReferenceFromUrl("gs://bucket/images/stars.jpg");

        // Create a reference from an HTTPS URL
        // Note that in the URL, characters are URL escaped!
        //StorageReference httpsReference = storage.getReferenceFromUrl("https://firebasestorage.googleapis.com/b/bucket/o/images%20stars.jpg");

    }

    private void uploadFile() {
        //创建引用
        StorageReference storageReference = storage.getReference();
        if (user == null) {
            Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        StorageReference faceBookImgReference = storageReference.child("user/" + user.getUid() + "/images/facebook.png");
        img_cloud.setDrawingCacheEnabled(true);
        img_cloud.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) img_cloud.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

        byte[] data = baos.toByteArray();
        UploadTask uploadTask = faceBookImgReference.putBytes(data);//开始上传
        uploadTask.addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double current = (100 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount();
                progress_upload.setVisibility(View.VISIBLE);
                progress_upload.setProgress((int) current);
                if (current >= 100) {
                    progress_upload.setVisibility(View.GONE);
                }
                Log.e(TAG, "上传进度：" + current + "%");
            }
        });
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, "上传失败，请重试：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                Log.e(TAG, "上传失败，请重试：" + e.getMessage());
                progress_upload.setVisibility(View.GONE);
            }
        });
        uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(MainActivity.this, "上传成功！", Toast.LENGTH_SHORT).show();
                Log.e(TAG, "上传成功");
                Log.i(TAG, "Metadata:" + taskSnapshot.getMetadata());
                Log.i(TAG, "UploadSessionUri:" + taskSnapshot.getUploadSessionUri());
            }
        });
    }

    private void localUpload() {

    }

    private void login() {
        List<AuthUI.IdpConfig> providers = new ArrayList<>();
        providers.add(new AuthUI.IdpConfig.GoogleBuilder().build());
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build();
        startActivityForResult(intent, RC_SIGN_IN);
    }

    class MyListAdapter extends BaseAdapter {
        List<StorageReference> referenceList;

        public MyListAdapter(List<StorageReference> referenceList) {
            this.referenceList = referenceList;
        }

        @Override
        public int getCount() {
            return referenceList.size();
        }

        @Override
        public Object getItem(int position) {
            return referenceList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            RefItemViewHolder viewHolder = null;
            StorageReference itemRef = (StorageReference) getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(MainActivity.this).inflate(R.layout.item_ref, null);
                viewHolder = new RefItemViewHolder();
                viewHolder.filePath = (TextView) convertView.findViewById(R.id.file_path);
                viewHolder.fileTitle = (TextView) convertView.findViewById(R.id.file_title);
                viewHolder.itemRefContainer = (RelativeLayout) convertView.findViewById(R.id.item_ref_container);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (RefItemViewHolder) convertView.getTag();
            }

            viewHolder.fileTitle.setText(itemRef.getName());
            viewHolder.filePath.setText(itemRef.getPath());
            viewHolder.itemRefContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            return convertView;
        }

        class RefItemViewHolder {
            RelativeLayout itemRefContainer;
            TextView fileTitle;
            TextView filePath;
        }
    }

}
