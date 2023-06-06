package com.example.restcontroller;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.dto.ApplyView;
import com.example.dto.Authnum;
import com.example.dto.Member;
import com.example.service.MailService;
import com.example.service.RedisUtil;
import com.example.service.apply.ApplyService;
import com.example.service.member.MemberService;

import lombok.extern.slf4j.Slf4j;


@RestController
@Slf4j
@RequestMapping(value = "/api/member")
public class RestMemberController {

    @Autowired MemberService mService;
    @Autowired MailService mailService;
    @Autowired RedisUtil redisUtil;
    @Autowired ApplyService aService;
    BCryptPasswordEncoder bcpe = new BCryptPasswordEncoder();

    final String format = "RestMemberController => {}";

    @GetMapping(value = "/idcheck.json")
    public Map<String, Integer> idcheckGET(@RequestParam(name = "id") String id) {

        //System.out.println(id);

        Map<String, Integer> retMap = new HashMap<>();

        int ret = mService.selectMemberIDCheck(id);
        
        retMap.put("ret", ret);

        return retMap;
    }

    @GetMapping(value = "/findid.json")
    public Map<String, String> findIdGET(@ModelAttribute Member obj) {

        //System.out.println(obj.toString());

        Map<String, String> retMap = new HashMap<>();

        Member ret = mService.selectFindMemberId(obj);

        //System.out.println(ret.getId());

        if(ret != null) {
            retMap.put("ret", ret.getId());
        }
        else {
            retMap.put("ret", "not");
        }

        return retMap;
    }

    @PostMapping(value = "/findpw.json")
    public Map<String, Object> findPwPOST(@RequestBody Member obj) throws MessagingException, UnsupportedEncodingException {

        log.info(format, obj.getEmail());

        Map<String, Object> retMap = new HashMap<>();

        String email = mailService.sendEmail(obj.getEmail());

        retMap.put("email", email);

        return retMap;

    }

    @PostMapping(value = "/verifyauthnum.json")
    public Map<String, Integer> verifyauthnumPOST(@RequestBody Authnum obj) {
    
        //log.info(format, obj.toString());
        //log.info(format, obj.getAuthnum());

        Map<String, Integer> retMap = new HashMap<>();

        String authnum = redisUtil.getData(obj.getEmail());
        //log.info(format, authnum);

        if(authnum == null) {
            retMap.put("status", -1);
        }
        if(obj.getAuthnum().equals(authnum)) {
            retMap.put("status", 200);
        }
        if(!obj.getAuthnum().equals(authnum)) {
            retMap.put("status", 0);
        }

        return retMap;
    }

    @PutMapping(value = "/findpwaction.json")
    public Map<String, Object> findpwactionPUT(@RequestBody Member obj) {

        Map<String, Object> retMap = new HashMap<>();

        log.info(format, obj.getPassword());

        try {
            Member member = mService.selectMemberOne(obj.getId());

            log.info(format, member.toString());

            member.setPassword(bcpe.encode(obj.getPassword()));

            int ret = mService.updateMemberPassword(member);

            retMap.put("ret", ret);
        }
        catch(Exception e) {
            e.printStackTrace();
            retMap.put("ret", -1);
        }

        return retMap;
    }

    @PostMapping(value = "/verifyPw.json")
    public Map<String, Object> verifyPwPOST(@RequestBody Member obj) {

        Map<String, Object> retMap = new HashMap<>();
        
        log.info(format, obj.getPassword());

        Member ret = mService.selectMemberOne(obj.getId());

        retMap.put("ret", 0);

        if(bcpe.matches(obj.getPassword(), ret.getPassword())) {
            retMap.put("ret", 1);
        }

        return retMap;

    }

    @PutMapping(value = "/updateinfo.json")
    public Map<String, Object> updateinfoPUT(@RequestBody Member obj) {

        Map<String, Object> retMap = new HashMap<>();
        
        log.info(format, obj.toString());

        int ret = mService.updateMemberOne(obj);

        retMap.put("ret", ret);

        if(ret == 1) {
            Member member = mService.selectMemberOne(obj.getId());
            retMap.put("member", member);
        }

        return retMap;
    }

    @PutMapping(value = "/updatepw.json")
    public Map<String, Object> updatepwPUT(@RequestBody Member obj) {

        Map<String, Object> retMap = new HashMap<>();

        Member member = mService.selectMemberOne(obj.getId());

        
        if(bcpe.matches(obj.getPassword(), member.getPassword())) {

            obj.setPassword(bcpe.encode(obj.getNewpassword()));
            int ret = mService.updateMemberPassword(obj);

            if(ret == 1) {

                retMap.put("status", 200);

            }
            else {
                retMap.put("status", 0);
            }

        }
        else {
            retMap.put("status", -1);
        }   

        return retMap;
    }

    @PutMapping(value = "/signout.json")
    public Map<String, Object> signoutPUT(@RequestBody Member obj) {

        Map<String, Object> retMap = new HashMap<>();

        Member member = mService.selectMemberOne(obj.getId());

        if(bcpe.matches(obj.getPassword(), member.getPassword())) {

            int ret = mService.deleteMemberOne(obj);

            if(ret == 1) {

                retMap.put("status", 200);

            }
            
            else {
                retMap.put("status", 0);
            }

        }
        else {
            retMap.put("status", -1);
        }   

        return retMap;

    }
    
    @GetMapping(value="/selectlist.json")
    public Map<String,Object> selectlistGET( 
        @RequestParam(name = "menu", defaultValue = "0") int menu,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @AuthenticationPrincipal User user ){

        Map<String, Object> map = new HashMap<>();
        String id = user.getUsername();
        int first = page*5-4;
        int last = page*5;
        long cnt = aService.countApplyList(id);
        List<ApplyView> list = new ArrayList<>();
        
            map.put("id",id);
            map.put("first",first);
            map.put("last",last);

            list = aService.selectApplyListById(map);

            map.put("list",list);
            map.put("pages", (cnt-1) / 5 + 1);
            map.put("status", 200);
                
        return map;
    }
    
}
