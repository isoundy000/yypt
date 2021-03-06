package com.zwsj.yypt.system.service;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zwsj.yypt.system.domain.SysMenu;
import com.zwsj.yypt.system.domain.SysMenuButton;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @创建人 zhk
 * @创建时间 2019-04-02
 * @描述
 */
public interface SysMenuService extends IService<SysMenu> {
    List<SysMenu> getMenusByRoleId(Long roleId);

    List<SysMenu> getMenutreeView();

    List<SysMenu> getlist();

    List<SysMenuButton> getMenuButtons(SysMenu sysMenu);

    @Transactional
    SysMenu updateOrAdd(SysMenu sysMenu);

    @Transactional
    void deleteMenu(SysMenu sysMenu);

    @Transactional
    void saveRoleMenu(Long roleId,String menuids);

}
