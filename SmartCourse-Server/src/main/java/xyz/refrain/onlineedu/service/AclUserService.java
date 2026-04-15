package xyz.refrain.onlineedu.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import xyz.refrain.onlineedu.constant.RS;
import xyz.refrain.onlineedu.mapper.AclUserMapper;
import xyz.refrain.onlineedu.model.entity.AclUserEntity;
import xyz.refrain.onlineedu.model.params.AclUserSearchParam;
import xyz.refrain.onlineedu.model.params.LoginParam;
import xyz.refrain.onlineedu.model.params.UpdatePasswordParam;
import xyz.refrain.onlineedu.model.params.UpdatePasswordWithAdminParam;
import xyz.refrain.onlineedu.model.securtiy.AclUserDetail;
import xyz.refrain.onlineedu.model.vo.PageResult;
import xyz.refrain.onlineedu.model.vo.R;
import xyz.refrain.onlineedu.model.vo.admin.AclUserVO;
import xyz.refrain.onlineedu.utils.IPUtils;
import xyz.refrain.onlineedu.utils.RUtils;
import xyz.refrain.onlineedu.utils.SessionUtils;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AclUserService {

	@Resource
	private AclUserMapper aclUserMapper;

	@Autowired
	private AliyunOssService aliyunOssService;

	/**
	 * 登录
	 */
	public R login(LoginParam param) {
		String username = param.getUsername().trim();
		String password = param.getPassword().trim();

		// 1. 查找用户
		AclUserEntity user = aclUserMapper.selectOne(Wrappers.lambdaQuery(AclUserEntity.class).eq(AclUserEntity::getUsername, username));
		if (Objects.isNull(user)) {
			System.err.println("DEBUG: User not found: [" + username + "]");
			return RUtils.fail(RS.USERNAME_ERROR);
		}
		
		// 调试日志：打印数据库里存的到底是什么
		System.out.println("DEBUG: Login attempt for [" + username + "]");
		System.out.println("DEBUG: Input Password: [" + password + "]");
		System.out.println("DEBUG: DB Password: [" + user.getPassword() + "]");

		if (!user.getEnable()) {
			return RUtils.fail(RS.ACCOUNT_DISABLED);
		}
		
		// 密码对比
		if (!password.equals(user.getPassword())) {
			System.err.println("DEBUG: Password mismatch!");
			return RUtils.fail(RS.PASSWORD_ERROR);
		}

		// 登录成功
		AclUserDetail detail = new AclUserDetail().convertFrom(user);
		String token = SessionUtils.generateToken(detail.getId());
		detail.setToken(token);
		SessionUtils.saveAclUser(detail);
		return RUtils.success("登录成功", new AclUserVO().convertFrom(detail));
	}

	public R logout() {
		boolean b = SessionUtils.removeAclUser(IPUtils.getRequest());
		return RUtils.commonFailOrNot(b ? 1 : 0, "登出");
	}

	public R info() {
		AclUserDetail aclUser = SessionUtils.getAclUser(IPUtils.getRequest());
		return RUtils.success("用户信息", new AclUserVO().convertFrom(aclUser));
	}

	public R create(AclUserDetail aclUser) {
		if (isUsernameExist(null, aclUser.getUsername())) { return RUtils.fail("用户名已存在"); }
		aclUser.setPassword(aclUser.getPassword());
		aclUser.setEnable(true);
		AclUserEntity aclUserEntity = aclUser.convertTo(new AclUserEntity());
		int i = aclUserMapper.insert(aclUserEntity);
		return RUtils.commonFailOrNot(i, "创建用户");
	}

	public R delete(int userId) {
		if (userId == 1) { return RUtils.fail("默认管理员无法删除"); }
		AclUserEntity entity = aclUserMapper.selectById(userId);
		if (Objects.nonNull(entity)) { aliyunOssService.delete(entity.getAvatar()); }
		int i = aclUserMapper.deleteById(userId);
		if (i > 0) { SessionUtils.removeAclUser(userId); }
		return RUtils.commonFailOrNot(i, "删除用户");
	}

	public R disable(int aclUserId) {
		if (aclUserId == 1) { return RUtils.fail("默认管理员无法禁用"); }
		int i = aclUserMapper.update(null, Wrappers.lambdaUpdate(AclUserEntity.class).eq(AclUserEntity::getId, aclUserId).set(AclUserEntity::getEnable, false));
		if (i > 0) { SessionUtils.removeAclUser(aclUserId); return RUtils.success("用户已禁用"); }
		return RUtils.fail("用户不存在或禁用失败");
	}

	public R enable(int aclUserId) {
		int i = aclUserMapper.update(null, Wrappers.lambdaUpdate(AclUserEntity.class).eq(AclUserEntity::getId, aclUserId).set(AclUserEntity::getEnable, true));
		if (i > 0) { return RUtils.success("用户已启用"); }
		return RUtils.fail("用户不存在或启用失败");
	}

	public R updateProfile(AclUserVO aclUserVO) {
		int i = aclUserMapper.update(null, Wrappers.lambdaUpdate(AclUserEntity.class).eq(AclUserEntity::getId, aclUserVO.getId()).set(StringUtils.hasText(aclUserVO.getNickname()), AclUserEntity::getNickname, aclUserVO.getNickname()).set(StringUtils.hasText(aclUserVO.getSign()), AclUserEntity::getSign, aclUserVO.getSign()));
		if (i > 0) {
			AclUserEntity aclUserEntity = aclUserMapper.selectById(aclUserVO.getId());
			AclUserDetail userDetail = new AclUserDetail().convertFrom(aclUserEntity);
			userDetail.setToken(SessionUtils.getTokenFromRequest(IPUtils.getRequest()));
			SessionUtils.saveAclUser(userDetail);
			return RUtils.success("用户信息更新成功", userDetail.convertTo(new AclUserDetail()));
		}
		return RUtils.fail("用户信息更新失败");
	}

	public R updateProfileWithAdmin(AclUserVO aclUserVO) {
		if (Objects.nonNull(aclUserVO.getUsername()) && isUsernameExist(aclUserVO.getId(), aclUserVO.getUsername())) { return RUtils.fail("用户名已存在"); }
		int i = aclUserMapper.update(null, Wrappers.lambdaUpdate(AclUserEntity.class).eq(AclUserEntity::getId, aclUserVO.getId()).set(StringUtils.hasText(aclUserVO.getUsername()), AclUserEntity::getUsername, aclUserVO.getUsername()).set(Objects.nonNull(aclUserVO.getRoleId()), AclUserEntity::getRoleId, aclUserVO.getRoleId()).set(StringUtils.hasText(aclUserVO.getNickname()), AclUserEntity::getNickname, aclUserVO.getNickname()).set(StringUtils.hasText(aclUserVO.getMark()), AclUserEntity::getMark, aclUserVO.getMark()));
		if (i > 0) {
			AclUserDetail userDetail = SessionUtils.getAclUser(aclUserVO.getId());
			if (Objects.nonNull(userDetail)) {
				AclUserEntity aclUserEntity = aclUserMapper.selectById(aclUserVO.getId());
				AclUserDetail aclUser = new AclUserDetail().convertFrom(aclUserEntity);
				aclUser.setToken(userDetail.getToken());
				SessionUtils.saveAclUser(aclUser);
			}
		}
		return RUtils.commonFailOrNot(i, "用户信息更新");
	}

	public R updatePassword(UpdatePasswordParam param) {
		if (!param.getNewPassword().equals(param.getConfirmNewPassword())) { return RUtils.fail(RS.INCONSISTENT_PASSWORDS); }
		AclUserDetail aclUser = SessionUtils.getAclUser(IPUtils.getRequest());
		int i = aclUserMapper.update(null, Wrappers.lambdaUpdate(AclUserEntity.class).eq(AclUserEntity::getId, aclUser.getId()).set(AclUserEntity::getPassword, param.getNewPassword()));
		if (i > 0) { aclUser.setPassword(param.getNewPassword()); SessionUtils.saveAclUser(aclUser); }
		return RUtils.commonFailOrNot(i, "密码更新");
	}

	public R updatePasswordWithAdmin(UpdatePasswordWithAdminParam param) {
		int i = aclUserMapper.update(null, Wrappers.lambdaUpdate(AclUserEntity.class).eq(AclUserEntity::getId, param.getUserId()).set(AclUserEntity::getPassword, param.getNewPassword()));
		if (i > 0) {
			AclUserDetail aclUser = SessionUtils.getAclUser(param.getUserId());
			if (Objects.nonNull(aclUser)) { aclUser.setPassword(param.getNewPassword()); SessionUtils.saveAclUser(aclUser); }
		}
		return RUtils.commonFailOrNot(i, "密码修改");
	}

	public R updateAvatar(MultipartFile file) throws IOException {
		if (file.getSize() > 1048576L) { return RUtils.fail("图片文件过大"); }
		String newAvatarUrl = aliyunOssService.upload(file);
		if (StringUtils.hasText(newAvatarUrl)) {
			AclUserDetail aclUser = SessionUtils.getAclUser(IPUtils.getRequest());
			int flag = aclUserMapper.update(null, Wrappers.lambdaUpdate(AclUserEntity.class).eq(AclUserEntity::getId, aclUser.getId()).set(AclUserEntity::getAvatar, newAvatarUrl));
			if (flag > 0) { aclUser.setAvatar(newAvatarUrl); SessionUtils.saveAclUser(aclUser); return RUtils.success("头像更新成功", newAvatarUrl); }
		}
		return RUtils.fail("头像更新出错");
	}

	public R list(AclUserSearchParam param) {
		Page<AclUserEntity> page = new Page<>(param.getCurrent(), param.getPageSize());
		Page<AclUserEntity> entityPage = aclUserMapper.selectPage(page, Wrappers.lambdaQuery(AclUserEntity.class).eq(Objects.nonNull(param.getEnable()), AclUserEntity::getEnable, param.getEnable()).eq(Objects.nonNull(param.getRoleId()), AclUserEntity::getRoleId, param.getRoleId()).like(StringUtils.hasText(param.getUsername()), AclUserEntity::getUsername, param.getUsername()));
		return RUtils.success("系统用户列表信息", covertToPageResult(entityPage));
	}

	public boolean isUsernameExist(Integer userId, String username) {
		Integer count = aclUserMapper.selectCount(Wrappers.lambdaQuery(AclUserEntity.class).ne(Objects.nonNull(userId) && userId > 0, AclUserEntity::getId, userId).eq(AclUserEntity::getUsername, username));
		return Objects.nonNull(count) && count > 0;
	}

	public PageResult<AclUserVO> covertToPageResult(IPage<AclUserEntity> entityIPage) {
		List<AclUserVO> voList = entityIPage.getRecords().stream().map(e -> (AclUserVO) new AclUserVO().convertFrom(e)).collect(Collectors.toList());
		return new PageResult<>(entityIPage.getTotal(), voList);
	}
}
