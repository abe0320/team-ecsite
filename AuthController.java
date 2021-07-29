package jp.co.internous.rainbow.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.Gson;

import jp.co.internous.rainbow.model.domain.MstUser;
import jp.co.internous.rainbow.model.form.UserForm;
import jp.co.internous.rainbow.model.mapper.MstUserMapper;
import jp.co.internous.rainbow.model.mapper.TblCartMapper;
import jp.co.internous.rainbow.model.session.LoginSession;

@RestController
@RequestMapping("/rainbow/auth")
public class AuthController {
	
	@Autowired
	private LoginSession loginSession;
	
	@Autowired
	private MstUserMapper userMapper;
	
	@Autowired
	private TblCartMapper cartMapper;
	
	private Gson gson = new Gson();
	
	
	/**
	 * ログイン処理をおこなう。
	 * @param f ユーザーフォーム
	 * @return ログインしたユーザー情報(JSON形式)
	 */
	@RequestMapping("/login")
	public String login(@RequestBody UserForm f) {
		MstUser user = userMapper.findByUserNameAndPassword(f.getUserName(), f.getPassword());
		
		int tmpUserId = loginSession.getTmpUserId();
		// 仮IDでカート追加されていれば、本ユーザーIDに更新する。
		if(user != null && tmpUserId != 0) {
			int count = cartMapper.findCountByUserId(tmpUserId);
			if (count > 0) {
				cartMapper.updateUserId(user.getId(), tmpUserId);
			}
		}
		
		if (user != null) {
			loginSession.setTmpUserId(0);
			loginSession.setLogined(true);
			loginSession.setUserId(user.getId());
			loginSession.setUserName(user.getUserName());
			loginSession.setPassword(user.getPassword());
		} else {
			loginSession.setLogined(false);
			loginSession.setUserId(0);
			loginSession.setUserName(null);
			loginSession.setPassword(null);
		}
		
		return gson.toJson(user);	
	}
	
	
	/**
	 * ログアウト処理をおこなう。
	 * @return 空文字
	 */
	@RequestMapping("/logout")
	public String logout() {
		loginSession.setTmpUserId(0);
		loginSession.setUserId(0);
		loginSession.setUserName(null);
		loginSession.setPassword(null);
		loginSession.setLogined(false);
		
		return "";
		
	}
	

	/**
	 * パスワード再設定をおこなう。
	 * @param f ユーザーフォーム
	 * @return 処理後のメッセージ
	 */
	@RequestMapping("/resetPassword")
	public String resetPassword(@RequestBody UserForm f) {
		String message = "パスワードが再設定されました。";
		String newPassword = f.getNewPassword();
		String newPasswordConfirm = f.getNewPasswordConfirm();
		
		MstUser user = userMapper.findByUserNameAndPassword(f.getUserName(), f.getPassword());
		if (user == null) {
			return "現在のパスワードが正しくありません。";
		}
		
		if (user.getPassword().equals(newPassword)) {
			return "現在のパスワードと同一文字列が入力されました。";
		}
		
		if (!newPassword.equals(newPasswordConfirm)) {
			return "新パスワードと確認用パスワードが一致しません。";
		}
		// mst_userとloginSessionのパスワードを更新する
		userMapper.updatePassword(user.getUserName(), f.getNewPassword());
		loginSession.setPassword(f.getNewPassword());
		
		
		return message;
	}
}
