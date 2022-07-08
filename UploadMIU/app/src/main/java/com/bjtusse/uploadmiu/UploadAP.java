package com.bjtusse.uploadmiu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.bjtusse.uploadmiu.entity.Ap;

import org.w3c.dom.Text;

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

public class UploadAP extends AppCompatActivity {

    TextView timeText;
    TextView areaIdText;
    ArrayList<Ap> apList = new ArrayList<>();
    private OkHttpClient okHttpClient = new OkHttpClient();
    ListView apListView;
    Button scanApButton;
    Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取 areaID
        Intent intent = getIntent();
        int data = intent.getIntExtra(Global.AREA_ID, -1);
        // 输出一下
        // Toast.makeText(this, String.valueOf(data), Toast.LENGTH_SHORT).show();

        setContentView(R.layout.activity_upload_ap);

        timeText = (TextView) findViewById(R.id.time);
        timeText.setText("还未扫描");
        areaIdText = (TextView) findViewById(R.id.area_id);
        areaIdText.setText("AREA " + data);

        if (ActivityCompat.checkSelfPermission(UploadAP.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(UploadAP.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);

        }

        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);


        scanApButton = findViewById(R.id.scan_aps);
        scanApButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        boolean succeedScan = wifiManager.startScan();
                        if (succeedScan) {
                            SimpleDateFormat sdf = new SimpleDateFormat();
                            sdf.applyPattern("yyyy-MM-dd HH:mm:ss a");
                            timeText.setText(sdf.format(new Date()));


                            List<ScanResult> scanResults = wifiManager.getScanResults();
                            List<String> apStrlist = new ArrayList<>();

                            // 获取扫描结果并展示
                            int i = 0;
                            for (ScanResult scanResult : scanResults) {
                                Ap ap = new Ap();
                                ap.setSsid(scanResult.SSID);
                                ap.setBssid(scanResult.BSSID);
                                apList.add(ap);
                                apStrlist.add("ssid："+scanResult.SSID + "  num:" + i);
                                i++;
                            }
                            apListView = findViewById(R.id.ap_list);
                            apListView.setAdapter(new ArrayAdapter<String>(UploadAP.this, android.R.layout.simple_expandable_list_item_1, apStrlist));

                        } else {
                            Toast.makeText(UploadAP.this, "扫描失败，请稍后", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );

        submitButton = (Button) findViewById(R.id.submit);
        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int areaIndex = data;
                String aps = JSONObject.toJSONString(apList);

                new Thread(new Runnable(){
                    @Override
                    public void run() {
                        try {
                            //1、封装请求体数据
                            FormBody formBody = new FormBody.Builder().add("aps",aps).add("areaId", String.valueOf(areaIndex)).build();
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
                                Toast.makeText(UploadAP.this, "添加成功！", Toast.LENGTH_SHORT).show();
                                Looper.loop();
                            }else{
                                Looper.prepare();
                                Toast.makeText(UploadAP.this, "添加失败！", Toast.LENGTH_SHORT).show();
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