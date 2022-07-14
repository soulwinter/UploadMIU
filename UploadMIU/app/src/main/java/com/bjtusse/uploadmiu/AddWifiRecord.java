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
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.alibaba.fastjson.JSONObject;
import com.bjtusse.uploadmiu.entity.Ap;
import com.bjtusse.uploadmiu.entity.Area;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;


public class AddWifiRecord extends AppCompatActivity {

    public static final int MIN_STRENGTH = -1000;
    private Spinner areaSpinner = null;
    private List<Ap> apList = new ArrayList<>();  //记录该区域的所有aps
    private List<Area> areaList = new ArrayList<>();
    private OkHttpClient okHttpClient = new OkHttpClient();
    private ArrayAdapter<CharSequence> adapterArea = null;
    private Button detectButton, uploadButton;
    private TextView aps_show, aps_test;
    private boolean scanSuccess;
    private TextView areaIdText;
    String apStr, strengthStr, x, y, area; // 上传wifi指纹的信息


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        Intent intent = getIntent();
        area =  String.valueOf(intent.getIntExtra(Global.AREA_ID, -1));
        setContentView(R.layout.activity_add_wifi_record);

        areaIdText = (TextView) findViewById(R.id.area_id2);
        areaIdText.setText("AREA " + area);

        aps_show =  (TextView) findViewById(R.id.aps_show);
        aps_show.setMovementMethod(ScrollingMovementMethod.getInstance());
        // initAreaSpinnner(); //初始化area下拉框
        initButton();  // 初始化button

    }

    // 初始化Button
    private void initButton(){
        detectButton = (Button) findViewById(R.id.detect_button);
        uploadButton = (Button) findViewById(R.id.testlocation);

        // 检测的button
        detectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getAps(); //一键获取有效ap，并显示在框中
            }
        });

        // 上传的button
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // 获取数据， TODO 写判断非空
                x = ((EditText)findViewById(R.id.pointX)).getText().toString();
                y = ((EditText)findViewById(R.id.pointY)).getText().toString();

                // 把ap和strength上传到数据库  TODO 为啥y永远为真？
                if( x==null || y==null) Toast.makeText(AddWifiRecord.this, "请输入坐标！", Toast.LENGTH_SHORT);
                else uploadAp(apStr,strengthStr, x, y, area);
            }
        });
    }

    // 获取并存储有效有ap的id和strength,显示在显示在ap_show中
    // 有效指：在ap库中存在记录
    void getAps(){

        //如果没有权限，进行动态分配
        if (ActivityCompat.checkSelfPermission(AddWifiRecord.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            ActivityCompat.requestPermissions(AddWifiRecord.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);

        }

        WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        wifiManager.setWifiEnabled(true);
        scanSuccess = wifiManager.startScan();

        aps_test = (TextView) findViewById(R.id.aps_test);
        if (!scanSuccess){
            aps_test.setText("扫描过于频繁，检测ap失败！");
            return;
        }

        aps_test.setText("ap检测成功！");

        List<Integer> strengthList = new ArrayList<>();  //记录最终要上传的strength
        List<Integer> apIdList = new ArrayList<>();   // 最终要上传的ap；总是(1,2,3,...,ap_num)，但我还是写上了
        List<ScanResult> scanResults = wifiManager.getScanResults();
        List<Integer> strengthDetectList = new ArrayList<>();  //记录检测到的strength
        List<Ap> apDetectList = new ArrayList<>();  // 当前检测到的ap
        TextView aps_show =(TextView) findViewById(R.id.aps_show);  //展示aps的框


        // 获取当前区域
//        area = String.valueOf(areaSpinner.getSelectedItem());
//        area =  area.split("id:")[1];

        // 获取ap库里，该区域的所有ap
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    //2、获取到请求的对象
                    Request request = new Request.Builder().url("http://114.116.234.63:8080/ap/listApByAreaId?areaId="+area).get().build();
                    //3、获取到回调的对象
                    Call call = okHttpClient.newCall(request);
                    //4、执行同步请求,获取到响应对象
                    Response response = call.execute();

                    //获取json字符串
                    String json = response.body().string();
                    System.out.println(json);
                    JSONObject jsonObject = JSONObject.parseObject(json);
                    String arrayStr = jsonObject.getString("data");

                    apList = JSONObject.parseArray(arrayStr, Ap.class);  //该area的所有ap


                    // 获取检测到的所有ap（有ssid和bssid信息）
                    for (ScanResult scanResult : scanResults) {
                        Ap ap = new Ap();
                        ap.setSsid(scanResult.SSID);
                        ap.setBssid(scanResult.BSSID);
                        apDetectList.add(ap);
                        strengthDetectList.add(scanResult.level);
                    }

                    // 比对ap库，将检测到的ap赋值对应强度，未检测到的ap默认强度为MIN_STRENGTH
                    boolean found;
                    for(int i = 0; i < apList.size(); i++){
                        found = false;
                        for(int j = 0; j < apDetectList.size(); j++){

                            if(apList.get(i).getSsid() == null || apDetectList.get(j).getSsid() == null) break;

                            // 匹配上了
                            if(Objects.equals(apList.get(i).getSsid(), apDetectList.get(j).getSsid())
                                    && Objects.equals(apList.get(i).getBssid(), apDetectList.get(j).getBssid())){
                                System.out.println("匹配上了："+i);

                                apIdList.add(i);
                                strengthList.add(strengthDetectList.get(j));
                                found = true;
                                break;
                            }
                        }
                        // 没匹配上，赋值为MIN_STRENGTH
                        if(!found){
                            System.out.println("没匹配上："+i);
                            apIdList.add(i);
                            strengthList.add(MIN_STRENGTH);
                        }
                    }

                    // TODO 修改为list比较好看
                    // 将有效的ap展示在框中
                    aps_show.setText("");
                    for(int i = 0; i < apList.size(); i ++){
                        aps_show.append("ap:" +apList.get(i).getSsid() + "   strength:" + strengthList.get(i) + "\n");
                    }

                    // 将apIdList和strengthList转化为string类型存储起来，格式为(1,2,3)
                    apStr = toStr(apIdList);
                    strengthStr = toStr(strengthList);




                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();



    }

    // 将list转化为string，string格式为(1,2,3)
    private String toStr(List<Integer> list){
        StringBuilder s = new StringBuilder("(");
        for(int i = 0; i < list.size(); i++){
            s.append(list.get(i).toString());
            if(i!=list.size()-1) s.append( ",");
        }
        s.append(")");
//        System.out.println("转化的ap和strength："+ s);
        return s.toString();
    }

    // 将wifi指纹上传到数据库
    private void uploadAp(String aps, String strength, String x, String y, String area){

        System.out.println("wifi指纹："+aps+"  "+strength+"  "+x+"  "+y+"  "+area);
        // 上传服务器
        new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    //1、封装请求体数据
                    FormBody formBody = new FormBody.Builder().add("x", x).add("y", y)
                            .add("aps",aps).add("strength",strength)
                            .add("areaId", area).build();
                    //2、获取到请求的对象
                    Request request = new Request.Builder().url("http://114.116.234.63:8080/wifiRecord/addWifiRecord").post(formBody).build();
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
                        Toast.makeText(AddWifiRecord.this, "wifi指纹上传成功！", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }else{
                        Looper.prepare();
                        Toast.makeText(AddWifiRecord.this, "wifi指纹上传失败！", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

}