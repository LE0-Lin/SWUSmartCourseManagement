package xyz.refrain.onlineedu.utils;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import xyz.refrain.onlineedu.constant.SessionConstant;
import xyz.refrain.onlineedu.model.securtiy.AclUserDetail;
import xyz.refrain.onlineedu.model.securtiy.EduTeacherDetail;
import xyz.refrain.onlineedu.model.securtiy.UctrMemberDetail;

import javax.servlet.http.HttpServletRequest;
import java.util.Set;
import java.util.UUID;

/**
 * Session Utils
 */
public class SessionUtils {

	// token 分隔符
	public static final String TOKEN_SEPARATOR = ":";

	// 格式 userId:uuid
	public static String generateToken(int userId) {
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		return userId + TOKEN_SEPARATOR + uuid;
	}

	public static String encodePassword(String password) {
		return password;
	}

	// 根据连接获取token
	public static String getTokenFromRequest(HttpServletRequest request) {
		String token = request.getHeader(SessionConstant.TOKEN_KEY);
		return StringUtils.hasText(token) ? token : null;
	}

	///////////////////////  系统用户   /////////////////////////////////////

	// 保存系统用户登录信息
	public static void saveAclUser(AclUserDetail aclUser) {
		removeAclUser(aclUser.getId());
		RedisUtils.set(SessionConstant.REDIS_NAMESPACE_ACL_USER + aclUser.getToken()
				, aclUser
				, SessionConstant.EXPIRE);
	}

	public static AclUserDetail getAclUser(String token) {
		return (AclUserDetail) RedisUtils.get(SessionConstant.REDIS_NAMESPACE_ACL_USER + token);
	}

	public static AclUserDetail getAclUser(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return (AclUserDetail) RedisUtils.get(SessionConstant.REDIS_NAMESPACE_ACL_USER + token);
	}

	public static AclUserDetail getAclUser(int userId) {
		Set<String> keys = RedisUtils.keys(SessionConstant.REDIS_NAMESPACE_ACL_USER + userId + "*");
		if (CollectionUtils.isEmpty(keys)) {
			return null;
		}
		AclUserDetail aclUser = null;
		for (String key : keys) {
			aclUser = (AclUserDetail) RedisUtils.get(key);
			break;
		}
		return aclUser;
	}

	public static boolean removeAclUser(String token) {
		String key = SessionConstant.REDIS_NAMESPACE_ACL_USER + token;
		long del = RedisUtils.del(key);
		return del > 0;
	}

	public static boolean removeAclUser(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return token != null
				&& RedisUtils.del(SessionConstant.REDIS_NAMESPACE_ACL_USER + token) > 0;
	}

	public static boolean removeAclUser(int userId) {
		Set<String> keys = RedisUtils.keys(SessionConstant.REDIS_NAMESPACE_ACL_USER + userId + "*");
		if (CollectionUtils.isEmpty(keys)) {
			return false;
		}
		long del = RedisUtils.del(keys.toArray(new String[0]));
		return del > 0;
	}

	public static boolean checkAclUserLogin(String token) {
		return RedisUtils.hasKey(SessionConstant.REDIS_NAMESPACE_ACL_USER + token);
	}

	public static boolean checkAclUserLogin(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return token != null
				&& RedisUtils.hasKey(SessionConstant.REDIS_NAMESPACE_ACL_USER + token);
	}


	///////////////////////  讲师   /////////////////////////////////////

	public static void saveTeacher(EduTeacherDetail detail) {
		removeTeacher(detail.getId());
		RedisUtils.set(SessionConstant.REDIS_NAMESPACE_TEACHER + detail.getToken()
				, detail
				, SessionConstant.EXPIRE);
	}

	public static EduTeacherDetail getTeacher(String token) {
		return (EduTeacherDetail) RedisUtils.get(SessionConstant.REDIS_NAMESPACE_TEACHER + token);
	}

	public static EduTeacherDetail getTeacher(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return (EduTeacherDetail) RedisUtils.get(SessionConstant.REDIS_NAMESPACE_TEACHER + token);
	}

	public static EduTeacherDetail getTeacher(int userId) {
		Set<String> keys = RedisUtils.keys(SessionConstant.REDIS_NAMESPACE_TEACHER + userId + "*");
		if (CollectionUtils.isEmpty(keys)) {
			return null;
		}
		EduTeacherDetail detail = null;
		for (String key : keys) {
			detail = (EduTeacherDetail) RedisUtils.get(key);
			break;
		}
		return detail;
	}

	public static boolean removeTeacher(String token) {
		String key = SessionConstant.REDIS_NAMESPACE_TEACHER + token;
		long del = RedisUtils.del(key);
		return del > 0;
	}

	public static boolean removeTeacher(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return token != null
				&& RedisUtils.del(SessionConstant.REDIS_NAMESPACE_TEACHER + token) > 0;
	}

	public static boolean removeTeacher(int userId) {
		Set<String> keys = RedisUtils.keys(SessionConstant.REDIS_NAMESPACE_TEACHER + userId + "*");
		if (CollectionUtils.isEmpty(keys)) {
			return false;
		}
		long del = RedisUtils.del(keys.toArray(new String[0]));
		return del > 0;
	}

	public static boolean checkTeacherLogin(String token) {
		return RedisUtils.hasKey(SessionConstant.REDIS_NAMESPACE_TEACHER + token);
	}

	public static boolean checkTeacherLogin(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return token != null
				&& RedisUtils.hasKey(SessionConstant.REDIS_NAMESPACE_TEACHER + token);
	}


	///////////////////////  网站会员   ////////////////////////////////////

	public static void saveMember(UctrMemberDetail member) {
		removeMember(member.getId());
		RedisUtils.set(SessionConstant.REDIS_NAMESPACE_MEMBER + member.getToken()
				, member
				, SessionConstant.EXPIRE);
	}

	public static UctrMemberDetail getMember(String token) {
		return (UctrMemberDetail) RedisUtils.get(SessionConstant.REDIS_NAMESPACE_MEMBER + token);
	}

	public static UctrMemberDetail getMember(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return (UctrMemberDetail) RedisUtils.get(SessionConstant.REDIS_NAMESPACE_MEMBER + token);
	}

	public static UctrMemberDetail getMember(int userId) {
		Set<String> keys = RedisUtils.keys(SessionConstant.REDIS_NAMESPACE_MEMBER + userId + "*");
		if (CollectionUtils.isEmpty(keys)) {
			return null;
		}
		UctrMemberDetail member = null;
		for (String key : keys) {
			member = (UctrMemberDetail) RedisUtils.get(key);
			break;
		}
		return member;
	}

	public static boolean removeMember(String token) {
		String key = SessionConstant.REDIS_NAMESPACE_MEMBER + token;
		long del = RedisUtils.del(key);
		return del > 0;
	}

	public static boolean removeMember(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return token != null
				&& RedisUtils.del(SessionConstant.REDIS_NAMESPACE_MEMBER + token) > 0;
	}

	public static boolean removeMember(int userId) {
		Set<String> keys = RedisUtils.keys(SessionConstant.REDIS_NAMESPACE_MEMBER + userId + "*");
		if (CollectionUtils.isEmpty(keys)) {
			return false;
		}
		long del = RedisUtils.del(keys.toArray(new String[0]));
		return del > 0;
	}

	public static boolean checkMemberLogin(String token) {
		return RedisUtils.hasKey(SessionConstant.REDIS_NAMESPACE_MEMBER + token);
	}

	public static boolean checkMemberLogin(HttpServletRequest request) {
		String token = getTokenFromRequest(request);
		return token != null
				&& RedisUtils.hasKey(SessionConstant.REDIS_NAMESPACE_MEMBER + token);
	}
}
