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
import android.telephony.TelephonyManager;
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
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

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
    private Button btn_real_time_db_update;
    private Button btn_real_time_db_read2;
    private Button btn_firebase_official_login;
    private Button btn_fire_store_upload;
    private Button btn_fire_store_download;
    private Button btn_fire_store_update;
    private boolean hasTryLogin = false;

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
        User testUser = new User("tom", 1001, 0);
        User testUser2 = new User("perks", 1002, 1);
        userList.add(testUser);
        userList.add(testUser2);
        for (int i = 0; i < 2; i++) {
            User user = new User("jack1", (1003 + i), 1);
            userList.add(user);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && !hasTryLogin) {
            login();
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
            hasTryLogin = true;
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
        btn_real_time_db_update = (Button) findViewById(R.id.btn_real_time_db_update);
        btn_real_time_db_update.setOnClickListener(this);
        btn_real_time_db_read2 = (Button) findViewById(R.id.btn_real_time_db_read2);
        btn_real_time_db_read2.setOnClickListener(this);
        btn_firebase_official_login = (Button) findViewById(R.id.btn_firebase_official_login);
        btn_firebase_official_login.setOnClickListener(this);
        btn_fire_store_upload = (Button) findViewById(R.id.btn_fire_store_upload);
        btn_fire_store_upload.setOnClickListener(this);
        btn_fire_store_download = (Button) findViewById(R.id.btn_fire_store_download);
        btn_fire_store_download.setOnClickListener(this);
        btn_fire_store_update = (Button) findViewById(R.id.btn_fire_store_update);
        btn_fire_store_update.setOnClickListener(this);
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
            case R.id.btn_real_time_db_update:
                testRTDB_update();
                break;
            case R.id.btn_real_time_db_read2:
                testRTDB_read2();
                break;
            case R.id.btn_firebase_official_login:
                loginWithMailPwd();
                break;
            case R.id.btn_fire_store_upload:
                fireStoreUpload();
                break;
            case R.id.btn_fire_store_download:
                fireStoreDownload();
                break;
            case R.id.btn_fire_store_update:
                fireStoreUpdate();
                break;
        }
    }

    private static final String MODULE_PATH = "cloud_vip";
    private void fireStoreUpdate() {
        if (!verifyLogin()) {
            return;
        }
        Map<String, Object> user = new HashMap<>();
        user.put("uuid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        user.put("expire_date", System.currentTimeMillis());
        user.put("email", FirebaseAuth.getInstance().getCurrentUser());
        user.put("mcc", 461);
        user.put("languages", "cn");
        user.put("location", Locale.getDefault().getCountry());
        //更新之前确保已有数据，先查询是否有数据
        FirebaseFirestore.getInstance().collection(MODULE_PATH).document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .update("expire_date", System.currentTimeMillis())
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.e(TAG, "更新成功");
                        } else {
                            Log.e(TAG, "更新失败:"+task.getException().getMessage());
                        }
                    }
                });
    }

    private void fireStoreDownload() {
        if (!verifyLogin()) {
            return;
        }
        DocumentReference rootRef = FirebaseFirestore.getInstance()
                .collection(MODULE_PATH)
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid());
        rootRef.get().addOnCompleteListener(MainActivity.this, new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    Log.e(TAG, "下载firestore成功");
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (documentSnapshot.exists()) {
                        Map<String, Object> resultMap = documentSnapshot.getData();
                        Iterator<Map.Entry<String, Object>> iterator = resultMap.entrySet().iterator();
                        while (iterator.hasNext()) {
                            Map.Entry<String, Object> entry = iterator.next();
                            Log.i(TAG, "key:" + entry.getKey() + " - value:" + entry.getValue().toString());
                        }
                    } else {
                        Log.e(TAG, "数据不存在");
                    }
                } else {
                    Log.e(TAG, "下载firestore失败:" + task.getException().getMessage());
                }
            }
        });
    }
    //目录结构：projects/firelogin-28d3c/databases/(default)/documents/cloud_vip/OZWWSq1Itmf3dIpMC1rAkD86r6y1
    private void fireStoreUpload() {
        if (!verifyLogin()) {
            return;
        }
        Map<String, Object> user = new HashMap<>();
        user.put("uuid", FirebaseAuth.getInstance().getCurrentUser().getUid());
        user.put("expire_date", System.currentTimeMillis());
        user.put("email", FirebaseAuth.getInstance().getCurrentUser().getEmail());
        user.put("mcc", 460);
        user.put("languages", "cn");
        user.put("location", Locale.getDefault().getCountry());
        //TelephonyManager.getSimCountryIso();
        FirebaseFirestore.getInstance()
                .collection(MODULE_PATH)
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .set(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.e(TAG, "上传成功");
                        }
                    }
                });
    }

    // 验证数据库是否初始化
    private static boolean verifyLogin() {
        // 验证是否登录
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            return false;
        }
        return true;
    }

    private void loginWithMailPwd() {
        EmailPasswordActivity.startActivity(MainActivity.this);
    }

    private void testRTDB_read2() {
        DatabaseReference rootRef = database.getReference();
        User user111 = new User("dada", new Random().nextInt(), 1);
        Query users = rootRef.child("users").child(user.getUid()).orderByKey();
        users.toString();
    }

    //
    private void testRTDB_update() {
        if (verifyLoginAndDBinit()) return;
        DatabaseReference rootRef = database.getReference();
        User user111 = new User("dada", new Random().nextInt(), 1);
        rootRef.child("users").child(user.getUid()).push().setValue(user111);
    }

    // 验证数据库是否初始化
    private boolean verifyLoginAndDBinit() {
        if (database == null) {
            Toast.makeText(MainActivity.this, "database未初始化", Toast.LENGTH_SHORT).show();
            Log.e(TAG, "database未初始化");
            return true;
        }
        if (user == null) {
            Toast.makeText(MainActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    private void testRTDB_read() {
        if (verifyLoginAndDBinit()) return;
        DatabaseReference rootRef = database.getReference();
        rootRef.child("users").child(user.getUid()).child("cloud_vip").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Iterator<DataSnapshot> iterator = dataSnapshot.getChildren().iterator();
                while (iterator.hasNext()) {
                    DataSnapshot next = iterator.next();
                    Object value = next.getValue();
                    Log.i(TAG, "遍历key：" + next.getKey() + "- value:" + next.getValue());
                }
                Log.i(TAG, "数据库变动- key:" + dataSnapshot.getKey() + "value:" + dataSnapshot.getValue());
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
                Log.i(TAG, "数据库查询- key:" + dataSnapshot.getKey() + "value:" + dataSnapshot.getValue());

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.i(TAG, "数据库查询- key:" + dataSnapshot.getKey() + "value:" + dataSnapshot.getValue());

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.i(TAG, "数据库查询- key:" + dataSnapshot.getKey() + "value:" + dataSnapshot.getValue());

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.i(TAG, "数据库查询- key:" + dataSnapshot.getKey() + "value:" + dataSnapshot.getValue());

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.i(TAG, "数据库查询- msg:" + databaseError.getMessage() + ",code:" + databaseError.getCode());
            }
        });

    }

    private void testRTDB_write() {
        if (verifyLoginAndDBinit()) return;

        DatabaseReference rootRef = database.getReference();
        Map<String, String> vipInfoMap = new HashMap<>();
        vipInfoMap.put("purchaseTime", String.valueOf(System.currentTimeMillis()));
        vipInfoMap.put("purchaseType", "sub_monthly_cloud_v1");//购买类型productId
        Calendar calendar = Calendar.getInstance();
        Date date = new Date(System.currentTimeMillis());
        calendar.setTime(date);
        calendar.add(Calendar.MONTH, 1);
        Date rollDate = calendar.getTime();
        vipInfoMap.put("expireTime", String.valueOf(rollDate.getTime()));
        rootRef.child("users").child(user.getUid()).child("cloud_vip").setValue(vipInfoMap).addOnCompleteListener(MainActivity.this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.e(TAG, "上传成功");
                } else {
                    Log.e(TAG, "上传失败");
                }
            }
        });
//        rootRef.child("users").child(user.getUid()).setValue(userList)
//                .addOnFailureListener(new OnFailureListener() {
//                    @Override
//                    public void onFailure(@NonNull Exception e) {
//                        Toast.makeText(MainActivity.this, "写入数据库失败", Toast.LENGTH_SHORT).show();
//                        Log.e(TAG, "写入数据库失败，E:" + e.getMessage());
//                    }
//                })
//                .addOnSuccessListener(new OnSuccessListener<Void>() {
//                    @Override
//                    public void onSuccess(Void aVoid) {
//                        Toast.makeText(MainActivity.this, "写入数据库成功", Toast.LENGTH_SHORT).show();
//                        Log.e(TAG, "写入数据库成功");
//                    }
//                });
    }

    private void listAll() {
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(new OnSuccessListener<GetTokenResult>() {
            @Override
            public void onSuccess(GetTokenResult getTokenResult) {
                Log.d("获取User token", getTokenResult.getToken());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("token", "fail：" + e.getMessage());
                e.printStackTrace();
            }
        });


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
        // 查询全部
        storageRef.listAll().addOnCompleteListener(this, new OnCompleteListener<ListResult>() {
            @Override
            public void onComplete(@NonNull Task<ListResult> task) {
                Log.i(TAG, task.getResult() + "");
            }
        });
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
        StorageReference faceBookImgReference = storageReference.child("user/" + user.getUid() + "/facebook.png");
        img_cloud.setDrawingCacheEnabled(true);
        img_cloud.buildDrawingCache();
        Bitmap bitmap = ((BitmapDrawable) img_cloud.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);

        byte[] data = baos.toByteArray();
        StorageMetadata storageMetadata = new StorageMetadata.Builder().setContentType("image/png")
                .setCustomMetadata("a", "b")
                .setCustomMetadata("b", "c")
                .build();
        UploadTask uploadTask = faceBookImgReference.putBytes(data, storageMetadata);//开始上传
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
        providers.add(new AuthUI.IdpConfig.EmailBuilder().build());
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
