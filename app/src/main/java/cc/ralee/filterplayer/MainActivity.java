package cc.ralee.filterplayer;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.opengl.GLSurfaceView;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Toast;

import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import org.angmarch.views.NiceSpinner;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static java.lang.Thread.sleep;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, View.OnClickListener, View.OnTouchListener {
    public static final String TAG = "MainActivity";
    //change your file path here

    // GLSurfaceView是Android应用程序中实现OpenGl画图的重要组成部分。
    // GLSurfaceView中封装了一个Surface,而android平台下关于图像的现实，差不多都是由Surface来实现的。
    private GLSurfaceView glSurfaceView ;
    private VideoRenderer videoRenderer;
    private FilePickerDialog dialog = null;
    private String videoPath = null;

    private boolean controlOn = true;

    static final int FILTER_NONE = 0;
    static final int FILTER_BLACK_WHITE = 1;
    static final int FILTER_BLUR = 2;
    static final int FILTER_SHARPEN = 3;
    static final int FILTER_EDGE_DETECT = 4;
    static final int FILTER_EMBOSS = 5;

    private Button pause;
    private Button selectFileBtn;
    private NiceSpinner niceSpinner;
    private LinearLayout control;
    static int PLAY_PAUSE_FLAG = 1;


    private static final String[] TYPES =  {".mp4",".mkv", ".avi", ".3gp", ".m4a" };
    private static final HashSet<String> SUPPORTED_TYPES = new HashSet<String>(Arrays.asList(TYPES));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        Log.d(TAG, "SAMPLE PAth: " + SAMPLE);
        setContentView(R.layout.activity_main);
        niceSpinner = (NiceSpinner)findViewById(R.id.cameraFilter_spinner);
        List<String> dataset = new LinkedList<>(Arrays.asList("正常 ","黑白 ", "模糊 ", "锐化 ", "边缘 ", "浮雕 "));
        niceSpinner.attachDataSource(dataset);
        LinearLayout listenControl = (LinearLayout) findViewById(R.id.listen_control);
        selectFileBtn = (Button)findViewById(R.id.open_file);
        control = (LinearLayout)findViewById(R.id.control);

        // 获取android.opengl.GLSurfaceView控件
        glSurfaceView = (GLSurfaceView) findViewById(R.id.glSurfaceView);
        SeekBar seekBar = (SeekBar) findViewById(R.id.seekbar);


        // 有了GLSurfaceView之后，就相当于我们有了画图的纸。现在我们所需要做的就是如何在这张纸上画图,所以我们需要一支笔。
        // Renderer是GLSurfaceView的内部静态接口，它就相当于在此GLSurfaceView上作画的笔。我们通过实现这个接口来“作画”。
        // 最后通过GLSurfaceView的setRenderer(GLSurfaceView.Renderer renderer)方法，就可以将纸笔关联起来了。
        // 实现Renderer需要实现它的三个接口：
        //      onSurfaceCreated(GL10 gl, EGLConfig config)、
        //      onSurfaceChanged(GL10 gl, int width, int height)、
        //      onDrawFrame(GL10 gl)。
        videoRenderer = new VideoRenderer(MainActivity.this, glSurfaceView, seekBar, videoPath);

        // 选择OpenGL版本以及设置Render
        glSurfaceView.setEGLContextClientVersion(2);
        glSurfaceView.setRenderer(videoRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
//        Log.d(TAG, SAMPLE);

        niceSpinner.setOnTouchListener(this);
        niceSpinner.setOnItemSelectedListener(this);
        niceSpinner.dismissDropDown();
        pause = (Button) findViewById(R.id.pause);
        // avoid clicking without video

        pause.setEnabled(false);

        pause.setOnClickListener(this);
        selectFileBtn.setOnClickListener(this);
        glSurfaceView.setOnClickListener(this);

        showMediaCodecInfo();
    }

    private void showMediaCodecInfo() {
        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] mediaCodecInfos = mediaCodecList.getCodecInfos();
        for (MediaCodecInfo mediaCodecInfo : mediaCodecInfos) {
            Log.d(TAG, "showMediaCodecInfo: " + mediaCodecInfo.getName());
            String[] supportedTypes = mediaCodecInfo.getSupportedTypes();
            for (String supportedType : supportedTypes) {
                Log.d(TAG, "showMediaCodecInfo: supportedType: " + supportedType);
            }
        }
    }

    private void setSelectDialog() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        String dir = "/sdcard/Download";
        properties.root = new File(dir);
        properties.error_dir = new File(dir);
        properties.offset = new File(dir);
        properties.extensions = null;
        dialog = new FilePickerDialog(MainActivity.this, properties);
        dialog.setTitle("Select a File");
        dialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                //files is the array of the paths of files selected by the Application User.
                videoPath = files[0]; // test
                String suffix = videoPath.substring(videoPath.lastIndexOf("."));
                if(!SUPPORTED_TYPES.contains(suffix)) {
                    Toast.makeText(MainActivity.this, "不支持的格式！", Toast.LENGTH_SHORT).show();
                    return;
                }
                Log.d(TAG, "videoPath" + videoPath);
                videoRenderer.changeVideoPathAndPlay(videoPath);
                pause.setEnabled(true);
//                Log.d(TAG, "clickable" + pause.isClickable());
                pause.setBackgroundResource(R.drawable.pause);
                videoRenderer.pausePlay(PLAY_PAUSE_FLAG);

            }
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, final int position, long id) {

        glSurfaceView.queueEvent(new Runnable() {
            @Override public void run() {
                // notify the renderer that we want to change the encoder's state
                videoRenderer.changeFilterMode(position);
            }
        });
    }


    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }



    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.open_file:// Open video file
                if(dialog == null) {
                    setSelectDialog();
                    Log.d(TAG, "Dialog setted");
                }
                dialog.show();
                Log.d(TAG, "New Video path" + videoPath);
                break;
            
            case R.id.glSurfaceView:
                Log.d(TAG, "onClick:  GLSurfaceView clicked");
                if(controlOn == false){
                    control.setVisibility(View.VISIBLE);
                    controlOn = true;
                }else{
                    controlOn = false;
                    control.setVisibility(View.INVISIBLE);
                }
                break;

            case R.id.pause:
                if(!pause.isClickable()) {
                    Log.d(TAG, "pause is not clickable!");
                    break;
                }


                Log.d(TAG, "pause button onClick: ");
                videoRenderer.pausePlay(PLAY_PAUSE_FLAG);
                if(PLAY_PAUSE_FLAG == 1) {
                    pause.setBackgroundResource(R.drawable.play);
                }else {
                    pause.setBackgroundResource(R.drawable.pause);
                }
                PLAY_PAUSE_FLAG *= -1;

                break;

            default:
                break;
        }
    }

    

    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoRenderer.onDestroy();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glSurfaceView.onPause();
        videoRenderer.pausePlay(1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glSurfaceView.onResume();
        videoRenderer.onResume();
    }


    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if(v.getId()!= R.id.glSurfaceView){
        }
        return false;
    }
}

