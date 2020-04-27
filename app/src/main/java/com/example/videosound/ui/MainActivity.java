package com.example.videosound.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Camera;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.example.videosound.R;
import com.example.videosound.utils.ApiUtil;
import com.example.videosound.utils.AudioCodec;
import com.example.videosound.utils.AudioUtil;
import com.example.videosound.utils.GlobalConfig;
import com.example.videosound.utils.RecordAudioUtil;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

//import com.buihha.audiorecorder.Mp3Recorder;
//import com.example.wang.audiorecordermp3.util.FileUtils;


public class MainActivity extends CheckPermissionsActivity {

    private static final String TAG = "xieyaoyan";
    private Camera mCamera;
    private CameraPreview mPreview;
    private MediaRecorder mMediaRecorder;
    private boolean isRecording = false;

    public static final int MEDIA_TYPE_IMAGE = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_AUDIO = 3;
    public static final int MEDIA_TYPE_AUDIO_PCM = 4;
    private int mId;
    private String mPath;
    private String mp4Name;
    private String audioName;

    private String audioPath = Environment.getExternalStorageDirectory() + "/AudioRecorderMp3/recorder/";
    private EditText et_idcard;

    private RecordAudioUtil audioUtil = new RecordAudioUtil();
    Button captureButton;
    private MediaRecorder mediaSoundRecorder;
    FrameLayout preview;
    private String pcmPath = Environment.getExternalStorageDirectory().getPath() + "/demo/test1.aac";
    private String dirPath = Environment.getExternalStorageDirectory().getPath() + "/videosoud";
    private String mp4_Path = dirPath + "/output_video.mp4";
    private String audio_Path;
    private MediaExtractor mediaExtractor;
    MediaMuxer mediaMuxer;

    private static final int TIMEOUT_US = 1000;
    private MediaExtractor mExtractor;
    private MediaCodec mDecoder;
    //    private String outputpath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/zzz.pcm";
    private FileOutputStream fos;
    private boolean eosReceived;
    private int mSampleRate = 0;
    int channel = 0;
    MediaCodec mediaDecode;
    ByteBuffer[] decodeInputBuffers;
    ByteBuffer[] decodeOutputBuffers;
    MediaCodec.BufferInfo decodeBufferInfo;
    private AudioRecord audioRecord = null;  // 声明 AudioRecord 对象
    private int recordBufSize = 0; // 声明recoordBufffer的大小字段

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        CrashReport.initCrashReport(getApplicationContext(), "e873a52b83", true);
        et_idcard = (EditText) findViewById(R.id.et_idcard);
        captureButton = (Button) findViewById(R.id.button_capture);
        captureButton.setOnClickListener(listener);
    }

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.button_capture:
                    if (isRecording) {
                        Toast.makeText(MainActivity.this, "录制停止", 1000).show();
                        isRecording = false;
                        mCamera.lock();
                        releaseMediaRecorder();
                        stopRecord();
                        ((Button) v).setText("Capture");
//                        AudioUtil.getInstance().stopRecord();
//                        AudioUtil.getInstance().convertWaveFile(et_idcard.getText().toString());
//                        upMultiLoad();
//                        extractorAudio();
//                        initMediaDecode();
                        Android_Async_Http_Post();
                    } else {
                        if (TextUtils.isEmpty(et_idcard.getText().toString())) {
                            Toast.makeText(MainActivity.this, "请输入身份证号码", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startRecord();
                        prepareVideoRecorder(mCamera);
//                        AudioUtil.getInstance().startRecord(et_idcard.getText().toString());
//                        AudioUtil.getInstance().recordData();
                        ((Button) v).setText("Stop");
                        Toast.makeText(MainActivity.this, "录制文件保存在:" + mPath, Toast.LENGTH_LONG).show();
                        isRecording = true;
                    }
                    break;
                default:
                    break;
            }
        }
    };

    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 0:
                    break;
            }
        }
    };

    /**
     * 开始录音
     */
    private void startSound() {
        mediaSoundRecorder = new MediaRecorder();
        mediaSoundRecorder.reset();
        // 设置音频录入源
        mediaSoundRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        // 设置录制音频的输出格式
        mediaSoundRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        // 设置音频的编码格式
        mediaSoundRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mediaSoundRecorder.setAudioSamplingRate(44100);
        //设置音质频率
        mediaSoundRecorder.setAudioEncodingBitRate(96000);
//        audioPath = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();
        mediaSoundRecorder.setOutputFile(audioPath);
        try {
            mediaSoundRecorder.prepare();
            mediaSoundRecorder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        mId = FindFrontCamera();
        System.out.println("mId = " + mId);
        try {
            mCamera = Camera.open(mId);
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, "摄像头正在使用", Toast.LENGTH_LONG).show();
            return;
        }
        mCamera.setFaceDetectionListener(new MyFaceDetectionListener());
        mPreview = new CameraPreview(MainActivity.this, mCamera, mId);
        preview = (FrameLayout) findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }


    @TargetApi(9)
    private int FindFrontCamera() {
        int cameraCount = 0;
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        cameraCount = Camera.getNumberOfCameras(); // get cameras number
        System.out.println("cameraCount = " + cameraCount);
        for (int camIdx = 0; camIdx < cameraCount; camIdx++) {
            Camera.getCameraInfo(camIdx, cameraInfo); // get camerainfo
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 代表摄像头的方位，目前有定义值两个分别为CAMERA_FACING_FRONT前置和CAMERA_FACING_BACK后置
                return camIdx;
            }
        }
        return -1;
    }


    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaRecorder();       // if you are using MediaRecorder, release it first
        releaseCamera();              // release the camera immediately on pause event
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.stop();
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            mCamera.lock();           // lock camera for later use
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.release();        // release the camera for other applications
            mCamera = null;
        }
    }

    private void stopPreviewAndFreeCamera() {

        if (mCamera != null) {
            // Call stopPreview() to stop updating the preview surface.
            mCamera.stopPreview();

            // Important: Call release() to release the mCamera for use by other
            // applications. Applications should release the mCamera immediately
            // during onPause() and re-open() it during onResume()).
            mCamera.release();

            mCamera = null;
        }
    }

    /**
     * 录像
     *
     * @param mCamera
     */
    private void prepareVideoRecorder(Camera mCamera) {
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);
//        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//       设置声音编码格式
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.HE_AAC);
        // 设置视频的编码格式
        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
//         设置视频的采样率，每秒4帧
        mMediaRecorder.setVideoFrameRate(4);
//        mMediaRecorder.setProfile(CamcorderProfile.get(mId, CamcorderProfile.QUALITY_480P));
        mPath = getOutputMediaFile(MEDIA_TYPE_VIDEO).toString();

//        outputpath = getOutputMediaFile(MEDIA_TYPE_AUDIO_PCM).toString();
        mMediaRecorder.setOutputFile(mPath);

        Surface surface = mPreview.getHolder().getSurface();
        mMediaRecorder.setPreviewDisplay(surface);

//        mMediaRecorder.setVideoSize(100, 50);//分辨率
//        mMediaRecorder.setVideoFrameRate(5);//帧数
        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            Toast.makeText(MainActivity.this, "开始录像", 1000).show();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
        }
    }

    /**
     * Create a file Uri for saving an image or video
     */
    private Uri getOutputMediaFileUri(int type) {
        return Uri.fromFile(getOutputMediaFile(type));
    }

    /**
     * Create a File for saving an image or video
     */
    private File getOutputMediaFile(int type) {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CameraApp");
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                return null;
            }
        }
        File mediaFile;
        String idCard = et_idcard.getText().toString();
        if (type == MEDIA_TYPE_IMAGE) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + idCard + "_IMG" + ".jpg");
        } else if (type == MEDIA_TYPE_VIDEO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + idCard + "_VID" + ".mp4");
        } else if (type == MEDIA_TYPE_AUDIO) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + idCard + "_Aud" + ".aac");
        } else if (type == MEDIA_TYPE_AUDIO_PCM) {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + idCard + "_Aud" + ".pcm");
        } else {
            return null;
        }
        return mediaFile;
    }


    class MyFaceDetectionListener implements Camera.FaceDetectionListener {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if (faces.length > 0) {
                Log.d("FaceDetection", "face detected: " + faces.length +
                        " Face 1 Location X: " + faces[0].rect.centerX() +
                        "Y: " + faces[0].rect.centerY());
            }
        }
    }


    public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
        private SurfaceHolder mHolder;
        private Camera mCamera;
        private int mId;

        public CameraPreview(Context context, Camera camera, int id) {
            super(context);
            mCamera = camera;
            mId = id;

            // Install a SurfaceHolder.Callback so we get notified when the
            // underlying surface is created and destroyed.
            mHolder = getHolder();
            mHolder.addCallback(this);
            // deprecated setting, but required on Android versions prior to 3.0
            mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        }

        public void surfaceCreated(SurfaceHolder holder) {
            // The Surface has been created, now tell the mCamera where to draw the preview.
            try {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
                startFaceDetection();
            } catch (IOException e) {
                Log.d(TAG, "Error setting mCamera preview: " + e.getMessage());
            }
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            // empty. Take care of releasing the Camera preview in your activity.
            // Surface will be destroyed when we return, so stop the preview.
            if (mCamera != null) {
                // Call stopPreview() to stop updating the preview surface.
                mCamera.stopPreview();
                mCamera.lock();
            }
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

            // If your preview can change or rotate, take care of those events here.
            // Make sure to stop the preview before resizing or reformatting it.

            if (mHolder.getSurface() == null) {// preview surface does not exist
                return;
            }

            // stop preview before making changes
            try {
                mCamera.stopPreview();
            } catch (Exception e) {
                // ignore: tried to stop a non-existent preview
            }

            // set preview size and make any resize, rotate or reformatting changes here
            setCameraDisplayOrientation(MainActivity.this, mId, mCamera);
            List<Camera.Size> supportedPreviewSizes = mCamera.getParameters().getSupportedPreviewSizes();
            for (Camera.Size size : supportedPreviewSizes) {
                System.out.println("Width = " + size.width + "  Height = " + size.height);
            }
            Camera.Parameters parameters = mCamera.getParameters();

            parameters.setPreviewSize(1920, 1080);
            //         parameters.setPictureSize(640, 320);
            mCamera.setParameters(parameters);
            // start preview with new settings
            try {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            } catch (Exception e) {
                Log.d(TAG, "Error starting mCamera preview: " + e.getMessage());
            }
        }

        public void setCameraDisplayOrientation(AppCompatActivity activity,
                                                int cameraId, Camera camera) {
            Camera.CameraInfo info =
                    new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, info);
            int rotation = activity.getWindowManager().getDefaultDisplay()
                    .getRotation();
            int degrees = 0;
            switch (rotation) {
                case Surface.ROTATION_0:
                    degrees = 0;
                    break;
                case Surface.ROTATION_90:
                    degrees = 90;
                    break;
                case Surface.ROTATION_180:
                    degrees = 180;
                    break;
                case Surface.ROTATION_270:
                    degrees = 270;
                    break;
            }

            int result;
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                result = (info.orientation + degrees) % 360;
                result = (360 - result) % 360;  // compensate the mirror
            } else {  // back-facing
                result = (info.orientation - degrees + 360) % 360;
            }
            camera.setDisplayOrientation(result);
        }
    }

    private void startFaceDetection() {
        // Try starting Face Detection
        Camera.Parameters params = mCamera.getParameters();

        // start face detection only *after* preview has started
        if (params.getMaxNumDetectedFaces() > 0) {
            // camera supports face detection, so can start it:
            System.out.println("XXX can ");
            mCamera.startFaceDetection();
        }
    }


    private void Android_Async_Http_Post() {
        AsyncHttpClient client = new AsyncHttpClient();
        String url = "添加服务器地址";
        RequestParams params = new RequestParams();
        params.put("idCard", et_idcard.getText().toString());
        try {
            params.put("videoFile", new File(mPath));
            params.put("audioFile", new File(audio_Path));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        client.post(url, params, new AsyncHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Toast.makeText(MainActivity.this, "双录成功！", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                Toast.makeText(MainActivity.this, "双录失败,请重新录制", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void upMultiLoad() {
        String et_icart = et_idcard.getText().toString().trim();
        File file = new File(mPath);
        File files = new File(audio_Path);
        MultipartBody.Builder builder = new MultipartBody.Builder().setType(MultipartBody.FORM);
        mp4Name = file.getName();
        audioName = files.getName();
        builder.addFormDataPart("videoFile", mp4Name, RequestBody.create(MediaType.parse("multipart/form-data"), file));
        builder.addFormDataPart("audioFile", audioName, RequestBody.create(MediaType.parse("multipart/form-data"), files));
        builder.addFormDataPart("idCard", et_icart);
        List<MultipartBody.Part> parts = builder.build().parts();
        ApiUtil.uploadMemberIcon(parts).enqueue(new Callback<String>() {//返回结果
            @Override
            public void onResponse(Call<String> call, Response<String> response) {
                if (response.body() != null) {
                    Log.e("==", response.body().toString());
                    Toast.makeText(MainActivity.this, "双录成功！", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e("==", response.errorBody().toString());
                    Toast.makeText(MainActivity.this, "双录失败,请重新录制", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<String> call, Throwable t) {
                Toast.makeText(MainActivity.this, "双录失败,请重新录制", Toast.LENGTH_SHORT).show();
            }
        });
    }


    @Override
    protected void onDestroy() {
        releaseMediaRecorder();
        stopRecord();
        super.onDestroy();
    }

    /**
     * 不包含CRC，所以packetLen需要一帧的长度+7
     *
     * @param packet    一帧数据（包含adts头长度）
     * @param packetLen 一帧数据（包含adts头）的长度，
     */
    private static void addADTStoPacketOld(byte[] packet, int packetLen) {
        int profile = 2; // AAC LC
        int freqIdx = 8; //8 标识16000，取特定
        int channelCfg = 2; // 音频声道数为两个

        // fill in ADTS data
        packet[0] = (byte) 0xFF;//1111 1111
        packet[1] = (byte) 0xF9;//1111 1001  1111 还是syncword
        // 1001 第一个1 代表MPEG-2,接着00为常量，最后一个1，标识没有CRC

        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (channelCfg >> 2));
        packet[3] = (byte) (((channelCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }


    @SuppressLint("NewApi")
    private void extractorAudio() {
        mediaExtractor = new MediaExtractor();
        int audioIndex = -1;
        try {
            mediaExtractor.setDataSource(mPath);
            int trackCount = mediaExtractor.getTrackCount();
            for (int i = 0; i < trackCount; i++) {
                MediaFormat trackFormat = mediaExtractor.getTrackFormat(i);
                String string = trackFormat.getString(MediaFormat.KEY_MIME);
                if (string.startsWith("audio/")) {
                    audioIndex = i;
                }
            }
            mediaExtractor.selectTrack(audioIndex);
            MediaFormat trackFormat = mediaExtractor.getTrackFormat(audioIndex);
            mediaMuxer = new MediaMuxer(audio_Path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

            int i = mediaMuxer.addTrack(trackFormat);
            mediaMuxer.start();

            ByteBuffer byteBuffer = ByteBuffer.allocate(1024 * 1024);
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();

            long time;
            {
                mediaExtractor.readSampleData(byteBuffer, 0);
                if (mediaExtractor.getSampleFlags() == MediaExtractor.SAMPLE_FLAG_SYNC) {
                    mediaExtractor.advance();
                }
                mediaExtractor.readSampleData(byteBuffer, 0);
                long sampleTime = mediaExtractor.getSampleTime();
                mediaExtractor.advance();

                mediaExtractor.readSampleData(byteBuffer, 0);
                long sampleTime1 = mediaExtractor.getSampleTime();
                mediaExtractor.advance();

                time = Math.abs(sampleTime - sampleTime1);
            }

            mediaExtractor.unselectTrack(audioIndex);
            mediaExtractor.selectTrack(audioIndex);
            while (true) {
                int data = mediaExtractor.readSampleData(byteBuffer, 0);
                if (data < 0) {
                    break;
                }
                bufferInfo.size = data;
                bufferInfo.flags = mediaExtractor.getSampleFlags();
                bufferInfo.offset = 0;
                bufferInfo.presentationTimeUs += time;
                mediaMuxer.writeSampleData(i, byteBuffer, bufferInfo);
                mediaExtractor.advance();
            }
            Toast.makeText(this, "ok", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            mediaExtractor.release();
            mediaMuxer.stop();
            mediaMuxer.release();
        }
    }


    @SuppressLint("NewApi")
    public void startPlay(String path) throws IOException {
        eosReceived = false;
        //创建MediaExtractor对象用来解AAC封装
        mExtractor = new MediaExtractor();
        //设置需要MediaExtractor解析的文件的路径
        try {
            mExtractor.setDataSource(path);
        } catch (Exception e) {
            Log.e(TAG, "设置文件路径错误" + e.getMessage());
        }
        fos = new FileOutputStream(new File(audio_Path));
        MediaFormat format = mExtractor.getTrackFormat(0);
        if (format == null) {
            Log.e(TAG, "format is null");
            return;
        }
        //   chunkPCMDataContainer = new ArrayList<>();

        //判断当前帧的文件类型是否为audio
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime.startsWith("audio/")) {
            Log.d(TAG, "format ：" + format);
            //获取当前音频的采样率
            mExtractor.selectTrack(0);
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            //获取当前帧的通道数
            channel = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            //音频文件的长度
            long duration = format.getLong(MediaFormat.KEY_DURATION);
            Log.d(TAG, "length:" + duration / 1000000);
        }

        //创建MedioCodec对象
        mDecoder = MediaCodec.createDecoderByType(mime);
        //配置MedioCodec
        mDecoder.configure(format, null, null, 0);

        if (mDecoder == null) {
            Log.e(TAG, "Can't find video info");
            return;
        }
        //启动MedioCodec,等待传入数据
        mDecoder.start();

        new Thread(AACDecoderAndPlayRunnable).start();
    }

    Runnable AACDecoderAndPlayRunnable = new Runnable() {
        @Override
        public void run() {
            AACDecoderAndPlay();
            Android_Async_Http_Post();
        }
    };

    @SuppressLint("NewApi")
    private void AACDecoderAndPlay() {
        //MediaCodec在此ByteBuffer[]中获取输入数据
        ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
        //MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数
        ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();

        //用于描述解码得到的byte[]数据的相关信息
        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        //启动AudioTrack，这是个播放器，可以播放PCM格式的数据。如果有需要可以用到。不需要播放的直接删掉就可以了。
      /*  int buffsize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_STEREO,AudioFormat.ENCODING_PCM_16BIT);
        //创建AudioTrack对象
        AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                8000,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT,
                buffsize,
                AudioTrack.MODE_STREAM);

        //启动AudioTrack
        audioTrack.play();*/

        while (!eosReceived) {
            //获取可用的inputBuffer 网上很多填-1代表一直等待，0表示不等待 建议-1,避免丢帧。我这里随便填了个1000，也没有问题。具体我也不太清楚
            int inIndex = mDecoder.dequeueInputBuffer(TIMEOUT_US);
            if (inIndex >= 0) {
                ByteBuffer buffer = inputBuffers[inIndex];
                //从MediaExtractor中读取一帧待解的数据
                int sampleSize = mExtractor.readSampleData(buffer, 0);
                //小于0 代表所有数据已读取完成
                if (sampleSize < 0) {
                    Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                    mDecoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                } else {
                    //插入一帧待解码的数据
                    mDecoder.queueInputBuffer(inIndex, 0, sampleSize, mExtractor.getSampleTime(), 0);
                    //MediaExtractor移动到下一取样处
                    mExtractor.advance();
                }
                //从mediadecoder队列取出一帧解码后的数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
                //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
                int outIndex = mDecoder.dequeueOutputBuffer(info, TIMEOUT_US);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = mDecoder.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                        //  MediaFormat format = mDecoder.getOutputFormat();
                        //  audioTrack.setPlaybackRate(format.getInteger(MediaFormat.KEY_SAMPLE_RATE));
                        break;
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    default:
                        ByteBuffer outBuffer = outputBuffers[outIndex];
                        //BufferInfo内定义了此数据块的大小
                        final byte[] chunk = new byte[info.size];
                        //  createFileWithByte(chunk);
                        //将Buffer内的数据取出到字节数组中
                        outBuffer.get(chunk);
                        //数据取出后一定记得清空此Buffer MediaCodec是循环使用这些Buffer的，不清空下次会得到同样的数据
                        outBuffer.clear();
                        //  putPCMData(chunk);
                        try {
                            //将解码出来的PCM数据IO流存入本地文件。
                            fos.write(chunk);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        //  audioTrack.write(chunk,info.offset,info.offset+info.size);
                        //此操作一定要做，不然MediaCodec用完所有的Buffer后 将不能向外输出数据
                        mDecoder.releaseOutputBuffer(outIndex, false);
                        break;
                }
                //所有帧都解码完之后退出循环
                if (info.flags != 0) {
                    Log.i("AA", "转码成功++++++++++++++");
                    break;
                }

              /*
              所有帧都解码完并播放完之后退出循环
              if((info.flags&MediaCodec.BUFFER_FLAG_END_OF_STREAM)!=0){
                    break;
                }*/
            }
        }

        Log.i("AA", "转码成功");
        //释放MediaDecoder资源
        mDecoder.stop();
        mDecoder.release();
        mDecoder = null;

        //释放MediaExtractor资源
        mExtractor.release();
        mExtractor = null;

      /*  //释放AudioTrack资源
        audioTrack.stop();
        audioTrack.release();
        audioTrack = null;*/

        //关流
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        eosReceived = true;
    }

    /**
     * 初始化解码器
     */
    @SuppressLint("NewApi")
    private void initMediaDecode() {
        try {
            mediaExtractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
            mediaExtractor.setDataSource(audio_Path);//媒体文件的位置
            for (int i = 0; i < mediaExtractor.getTrackCount(); i++) {//遍历媒体轨道 此处我们传入的是音频文件，所以也就只有一条轨道
                MediaFormat format = mediaExtractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime.startsWith("audio")) {//获取音频轨道
//                    format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 200 * 1024);
                    mediaExtractor.selectTrack(i);//选择此音频轨道
                    mediaDecode = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                    mediaDecode.configure(format, null, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mediaDecode == null) {
            Log.e(TAG, "create mediaDecode failed");
            return;
        }
        mediaDecode.start();//启动MediaCodec ，等待传入数据
        decodeInputBuffers = mediaDecode.getInputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        decodeOutputBuffers = mediaDecode.getOutputBuffers();//MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        decodeBufferInfo = new MediaCodec.BufferInfo();//用于描述解码得到的byte[]数据的相关信息
    }

//    /**
//     * 将音频信息写入文件
//     */
//    private void writeAudioDataToFile() throws IOException {
//        OutputStream bos = null;
//        try {
//            bos = new BufferedOutputStream(new FileOutputStream(file));
//            ByteBuffer[] audioData = new ByteBuffer[decodeOutputBuffers];
//            while (mStatus == Status.STATUS_START) {
//                int readSize = mAudioRecord.read(audioData, 0, mBufferSizeInBytes);
//                if (readSize > 0) {
//                    try {
//                        bos.write(audioData, 0, readSize);
//                        if (mRecordStreamListener != null) {
//                            mRecordStreamListener.onRecording(audioData, 0, readSize);
//                        }
//                    } catch (IOException e) {
//                        Log.e(TAG, "writeAudioDataToFile", e);
//                    }
//                } else {
//                    Log.w(TAG, "writeAudioDataToFile readSize: " + readSize);
//                }
//            }
//            bos.flush();
//            if (mRecordStreamListener != null) {
//                mRecordStreamListener.finishRecord();
//            }
//        } finally {
//            if (bos != null) {
//                bos.close();// 关闭写入流
//            }
//        }
//    }


    public void startRecord() {
        final int minBufferSize = AudioRecord.getMinBufferSize(GlobalConfig.SAMPLE_RATE_INHZ, GlobalConfig.CHANNEL_CONFIG, GlobalConfig.AUDIO_FORMAT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, GlobalConfig.SAMPLE_RATE_INHZ,
                GlobalConfig.CHANNEL_CONFIG, GlobalConfig.AUDIO_FORMAT, minBufferSize);
        final byte data[] = new byte[minBufferSize];
        audio_Path = getOutputMediaFile(MEDIA_TYPE_AUDIO_PCM).toString();
        final File file = new File(audio_Path);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        if (file.exists()) {
            file.delete();
        }
        audioRecord.startRecording();
        isRecording = true;

        // TODO: 2018/3/10 pcm数据无法直接播放，保存为WAV格式。

        new Thread(new Runnable() {
            @Override
            public void run() {

                FileOutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                if (null != os) {
                    while (isRecording) {
                        int read = audioRecord.read(data, 0, minBufferSize);
                        // 如果读取音频数据没有出现错误，就将数据写入到文件
                        if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                            try {
                                os.write(data);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        Log.i(TAG, "run: close file output stream !");
                        os.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stopRecord() {
        // 释放资源
        if (null != audioRecord) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            //recordingThread = null;
        }
    }
}
