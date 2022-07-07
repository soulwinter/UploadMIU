package com.bjtusse.uploadmiu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import android.os.Looper;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;


import com.alibaba.fastjson.JSONObject;
import com.bjtusse.uploadmiu.entity.Ap;
import com.bjtusse.uploadmiu.entity.Area;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class AddAp extends AppCompatActivity {

    private Spinner apSpinner = null;
    private TextView timeText;
    private ArrayAdapter<CharSequence> adapterAp = null;
    private Spinner areaSpinner = null;
    private ArrayAdapter<CharSequence> adapterArea = null;

    private OkHttpClient okHttpClient = new OkHttpClient();

    private List<Ap> apList = new ArrayList<>();
    private List<Area> areaList = new ArrayList<>();



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_ap);

        //初始化提交按钮
        initButton();

        //初始化ap下拉框
        apSpinner =  (Spinner)findViewById(R.id.ap_spinner);

        //如果没有权限，进行动态分配
        if (ActivityCompat.checkSelfPermission(AddAp.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(AddAp.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);

        }

        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        wifiManager.startScan();

        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<String> apStrlist = new ArrayList<>();

        int i = 0;

        timeText = (TextView) findViewById(R.id.time);
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");
        timeText.setText(sdf.format(new Date()));


        for (ScanResult scanResult : scanResults) {
            Ap ap = new Ap();
            ap.setSsid(scanResult.SSID);
            ap.setBssid(scanResult.BSSID);
            apList.add(ap);
            apStrlist.add("ssid："+scanResult.SSID + "  num:" + i);
            i++;
        }
        this.adapterAp = new ArrayAdapter<CharSequence>(this,
                android.R.layout.simple_spinner_dropdown_item,apStrlist.toArray(new String[0]));
        apSpinner.setAdapter(adapterAp);



        //初始化area下拉框
        areaSpinner =  (Spinner)findViewById(R.id.area_spinner);


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
                        areaStrlist.add("name:"+area.getName()+"  id:"+area.getId());
                    }
                    adapterArea = new ArrayAdapter<CharSequence>(AddAp.this,
                            android.R.layout.simple_spinner_dropdown_item,areaStrlist.toArray(new String[0]));

                    //不能在子线程操作ui
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            areaSpinner.setAdapter(adapterArea);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }


    private void initButton(){
        Button submitButton = (Button) findViewById(R.id.submit_button);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int areaIndex = (int)areaSpinner.getSelectedItemId();

                String aps = JSONObject.toJSONString(apList);

                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            //1、封装请求体数据
                            FormBody formBody = new FormBody.Builder().add("aps",aps).add("areaId",areaList.get(areaIndex).getId().toString()).build();
                            //2、获取到请求的对象
                            Request request = new Request.Builder().url("http://114.116.234.63:8080/ap/addAp").post(formBody).build();
                            //3、获取到回调的对象
                            Call call = okHttpClient.newCall(request);
                            //4、执行同步请求,获取到响应对象
                            Response response = call.execute();

                            //获取json字符串
                            String json = response.body().string();
                            JSONObject jsonObject = JSONObject.parseObject(json);
                            Integer code = jsonObject.getInteger("code");
                            if (code == 200){
                                Looper.prepare();
                                Toast.makeText(AddAp.this, "添加成功！", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }else{
                                Looper.prepare();
                                Toast.makeText(AddAp.this, "添加失败！", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
    }
}