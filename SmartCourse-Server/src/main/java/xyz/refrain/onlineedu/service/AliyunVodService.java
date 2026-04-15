package xyz.refrain.onlineedu.service;

import cn.hutool.core.io.file.FileNameUtil;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.vod.model.v20170321.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import xyz.refrain.onlineedu.config.properties.AliyunProperties;

import java.io.InputStream;
import java.util.List;

/**
 * Aliyun Vod Service
 *
 * @author SWU
 */
@Service
@Slf4j
public class AliyunVodService {

	@Autowired
	private DefaultAcsClient vodClient;

	@Autowired
	private AliyunProperties aliyunProperties;

	/**
	 * 上传视频
	 *
	 * @return 返回视频Id
	 */
	public String uploadVideo(MultipartFile file) {
		log.info("Mock Upload Video: " + file.getOriginalFilename());
		return "mock-video-id";
	}

	/**
	 * 获取视频信息
	 */
	public GetPlayInfoResponse getVideoInfo(String videoId) {
		// 创建获取视频地址request和response
		GetPlayInfoRequest request = new GetPlayInfoRequest();
		// 向request对象里面设置视频ID
		request.setVideoId(videoId);
		// 调用初始化对象里面的方法，传递request，获取数据
		try {
			return vodClient.getAcsResponse(request);
		} catch (Exception e) {
			log.info("获取视频信息失败", e.getCause());
			return null;
		}
	}

	/**
	 * 获取视频播放地址
	 *
	 * @return 视频播放地址
	 */
	public String getPlayUrl(String videoId) {
		// 创建获取视频地址request和response
		GetPlayInfoRequest request = new GetPlayInfoRequest();
		// 向request对象里面设置视频ID
		request.setVideoId(videoId);
		// 调用初始化对象里面的方法，传递request，获取数据
		try {
			GetPlayInfoResponse response = vodClient.getAcsResponse(request);
			for (GetPlayInfoResponse.PlayInfo playInfo : response.getPlayInfoList()) {
				return playInfo.getPlayURL();
			}
		} catch (Exception e) {
			log.info("获取视频播放地址失败", e.getCause());
		}
		return null;
	}

	/**
	 * 获取视频凭证
	 * 当音频被加密时，无法直接通过地址访问，此时可以通过视频凭证访问，视频凭证由阿里云API生成
	 *
	 * @return 视频凭证
	 */
	public String getPlayAuth(String videoId) {
		// 创建初始化对象
		GetVideoPlayAuthRequest request = new GetVideoPlayAuthRequest();
		// 向request对象设置视频ID值
		request.setVideoId(videoId);
		// 调用初始化对象里的方法得到凭证
		try {
			GetVideoPlayAuthResponse response = vodClient.getAcsResponse(request);
			return response.getPlayAuth();
		} catch (Exception e) {
			log.info("获取视频凭证失败", e.getCause());
		}
		return null;
	}

	/**
	 * 删除视频
	 */
	public boolean deleteVideos(List<String> videoIdList) {
		try {
			//创建删除视频request对象
			DeleteVideoRequest request = new DeleteVideoRequest();
			//videoIdList值转换成 1,2,3
			String videoIds = StringUtils.join(videoIdList.toArray(), ",");
			System.out.println(videoIds);
			//向request设置视频id
			request.setVideoIds(videoIds);
			//调用初始化对象的方法实现删除
			vodClient.getAcsResponse(request);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			log.info("删除视频失败", e.getCause());
		}
		return false;
	}


}
