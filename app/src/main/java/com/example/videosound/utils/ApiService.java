package com.example.videosound.utils;

import java.util.List;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

/**
 * @author jianghuizhong
 * @describe
 * @date 2019/6/19
 */

public interface ApiService {
    /**
     * 图片上传
     *
     * @param partList
     * @return
     */
    @Multipart
    @POST("/upload")
    Call<String> uploadMemberIcon(@Part List<MultipartBody.Part> partList);
}
