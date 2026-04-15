package xyz.refrain.onlineedu.utils;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.UUID;

@Slf4j
@Component
public class VideoUtil {

	/**
	 * 获取视频时长(时分秒)
	 */
	public static String ReadVideoTimeMs(MultipartFile file) {
		return "00:00:00";
	}

	/**
	 * 删除文件
	 */
	private static void deleteFile(File... files) {
		for (File file : files) {
			if (file.exists()) {
				file.delete();
			}
		}
	}
}
