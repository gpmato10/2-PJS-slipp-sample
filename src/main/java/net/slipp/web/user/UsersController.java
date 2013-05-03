package net.slipp.web.user;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.slipp.domain.user.PasswordDto;
import net.slipp.domain.user.SocialUser;
import net.slipp.service.user.SocialUserService;
import net.slipp.social.security.AutoLoginAuthenticator;
import net.slipp.support.web.argumentresolver.LoginUser;
import net.slipp.web.UserForm;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
@RequestMapping("/users")
public class UsersController {
    @Resource(name = "socialUserService")
    private SocialUserService userService;

    @Resource(name = "autoLoginAuthenticator")
    private AutoLoginAuthenticator autoLoginAuthenticator;

    @RequestMapping("/login")
    public String login(Model model) {
        model.addAttribute("user", new UserForm());
        return "users/login";
    }

    @RequestMapping(value = "", method = RequestMethod.POST)
    public String create(UserForm user, HttpServletRequest request, HttpServletResponse response) {
        SocialUser socialUser = userService.createUser(user.getUserId(), user.getNickName(), user.getEmail());
        autoLoginAuthenticator.login(socialUser.getUserId(), socialUser.getRawPassword());
        return "redirect:/";
    }

    @RequestMapping("/fblogout")
    public String logout() {
        return "users/fblogout";
    }

    @RequestMapping("/{id}")
    public String profileById(@PathVariable Long id) throws Exception {
        SocialUser socialUser = userService.findById(id);
        return String.format("redirect:/users/%d/%s", id, socialUser.getUserId());
    }

    @RequestMapping("/{id}/{userId}")
    public String profile(@PathVariable Long id, @PathVariable String userId, Model model) throws Exception {
        model.addAttribute("socialUser", userService.findById(id));
        return "users/profile";
    }

    @RequestMapping("/changepassword/{id}")
    public String changePasswordForm(@LoginUser SocialUser loginUser, @PathVariable Long id, Model model)
            throws Exception {
        SocialUser socialUser = userService.findById(id);
        if (!loginUser.isSameUser(socialUser)) {
            throw new IllegalArgumentException("You cann't change another user!");
        }
        
        model.addAttribute("socialUser", socialUser);
        model.addAttribute("password", new PasswordDto(id));
        return "users/changepassword";
    }

    @RequestMapping(value = "/changepassword/{id}", method = RequestMethod.POST)
    public String changePassword(@LoginUser SocialUser loginUser, @PathVariable Long id, PasswordDto password, Model model) throws Exception {
        SocialUser socialUser = userService.findById(id);
        
        if (!loginUser.isSameUser(socialUser)) {
            throw new IllegalArgumentException("You cann't change another user!");
        }
            
        try {
            userService.changePassword(loginUser, password);
            return "redirect:/users/logout";
        } catch (BadCredentialsException e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("socialUser", socialUser);
            model.addAttribute("password", new PasswordDto(id));
            return "users/changepassword";            
        }
    }
}
