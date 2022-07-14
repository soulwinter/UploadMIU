package com.bjtusse.uploadmiu;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.bjtusse.uploadmiu.entity.Area;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private OkHttpClient okHttpClient = new OkHttpClient();
    private ListView areaListView;
    private List<Area> areaList = new ArrayList<>();
    private int choseAreaId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);


        setContentView(R.layout.activity_main);


        new Thread(new Runnable(){
            @Override
            public void run() {


                try {
                    //2、获取到请求的对象
                    Request request = new Request.Builder().url("http://114.116.234.63:8080/area/listArea").get().build();
                    //3、获取到回调的对象
                    Call call = okHttpClient.newCall(request);

                    //4、执行同步请求,获取到响应对象
                    Response response = call.execute();


                    //获取json字符串
                    String json = response.body().string();
                    System.out.println(json);
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    String arrayStr = jsonObject.getString("data");

                    areaList = JSONObject.parseArray(arrayStr, Area.class);

                    List<String> areaStrlist = new ArrayList<>();
                    for (Area area : areaList) {
                        areaStrlist.add(area.getId() + ": " + area.getName());
                    }
                    Log.v("testByHcb", String.valueOf(areaStrlist.size()));


                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            areaListView = findViewById(R.id.AreaList);
                            areaListView.setAdapter(new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_expandable_list_item_1, areaStrlist));

                            areaListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                                    // 获取选择的区域 id
                                    TextView textView = (TextView) view;
                                    String originAreaString = textView.getText().toString();
                                    choseAreaId = Integer.parseInt(originAreaString.substring(0, originAreaString.indexOf(":")));


                                }
                            });

                        }
                    });





                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();



        Button uploadAP = (Button) findViewById(R.id.uploadAP);
        uploadAP.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (choseAreaId == -1)
                        {
                            Toast.makeText(MainActivity.this, "还未选择 Area", Toast.LENGTH_SHORT).show();
                        } else {
                            // 将 id 传输给 uploadAP
                            Intent intent = new Intent(MainActivity.this, UploadAP.class);
                            // intent.setData(Uri.parse("area_id: " + choseAreaId));
                            intent.putExtra(Global.AREA_ID, choseAreaId);
                            startActivity(intent);
                        }

                    }
                }
        );

        Button uploadWifiRecord = (Button) findViewById(R.id.uploadwifirecord);
        uploadWifiRecord.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (choseAreaId == -1)
                        {
                            Toast.makeText(MainActivity.this, "还未选择 Area", Toast.LENGTH_SHORT).show();
                        } else {
                            // 将 id 传输给 uploadAP
                            Intent intent = new Intent(MainActivity.this, AddWifiRecord.class);
                            // intent.setData(Uri.parse("area_id: " + choseAreaId));
                            intent.putExtra(Global.AREA_ID, choseAreaId);
                            startActivity(intent);
                        }
                    }
                }
        );

        Button addAreaButton = (Button) findViewById(R.id.add_area);
        addAreaButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(MainActivity.this, AddArea.class);
                        // intent.setData(Uri.parse("area_id: " + choseAreaId));
                        startActivity(intent);
                    }
                }
        );


    }
}