package com.mine.oa.service.impl;

import java.util.*;

import com.mine.oa.entity.UserRolePO;
import com.mine.oa.mapper.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mine.oa.constant.OaConstants;
import com.mine.oa.dto.LoginInfoDTO;
import com.mine.oa.dto.RoleQueryDTO;
import com.mine.oa.entity.MenuPO;
import com.mine.oa.entity.RoleMenu;
import com.mine.oa.entity.RolePO;
import com.mine.oa.exception.InParamException;
import com.mine.oa.service.RoleService;
import com.mine.oa.vo.CommonResultVo;

/***
 *
 * 〈一句话功能简述〉<br>
 * 〈功能详细描述〉
 *
 * @author liupeng
 * @see [相关类/方法]（可选）
 * @since [产品/模块版本] （可选）
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleMapper roleMapper;
    @Autowired
    private MenuMapper menuMapper;
    @Autowired
    private RoleMenuMapper roleMenuMapper;
    @Autowired
    private UserRoleMapper userRoleMapper;

    @Override
    public CommonResultVo insert(RolePO role) {
        if (role == null || StringUtils.isBlank(role.getName())) {
            throw new InParamException();
        }
        role.setCreateUserId(LoginInfoDTO.get().getId());
        if (roleMapper.insertSelective(role) < 1) {
            throw new InParamException();
        }
        return new CommonResultVo().successMsg("新增成功");
    }

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CommonResultVo delete(Integer id) {
        if (id == null) {
            throw new InParamException();
        }
        UserRolePO userRole = new UserRolePO();
        userRole.setRoleId(id);
        if (userRoleMapper.selectCount(userRole) > 0) {
            return new CommonResultVo().warn("无法删除，该角色下存在用户！");
        }
        if (this.updateState(id, OaConstants.DELETE_STATE) < 1) {
            throw new InParamException();
        }
        return new CommonResultVo().successMsg("删除成功");
    }

    @Override
    public CommonResultVo enable(Integer id) {
        if (id == null || this.updateState(id, OaConstants.NORMAL_STATE) < 1) {
            throw new InParamException();
        }
        return new CommonResultVo().successMsg("启用成功");
    }

    @Override
    public CommonResultVo update(RolePO role) {
        if (!ObjectUtils.allNotNull(role, role.getId()) || StringUtils.isBlank(role.getName())) {
            throw new InParamException();
        }
        RolePO updateRole = new RolePO();
        updateRole.setId(role.getId());
        updateRole.setUpdateTime(new Date());
        updateRole.setUpdateUserId(LoginInfoDTO.get().getId());
        if (roleMapper.updateByPrimaryKeySelective(updateRole) < 1) {
            throw new InParamException();
        }
        return new CommonResultVo().successMsg("修改成功");
    }

    @Override
    public CommonResultVo findPageByParam(RoleQueryDTO roleQuery) {
        PageHelper.startPage(roleQuery.getCurrent(), roleQuery.getPageSize());
        RolePO role = new RolePO();
        role.setName(StringUtils.isBlank(roleQuery.getName()) ? null : roleQuery.getName());
        role.setState(roleQuery.getState());
        return new CommonResultVo<>().success(new PageInfo<>(roleMapper.select(role)));
    }

    @Override
    public CommonResultVo findMenu(Integer id) {
        if (id == null) {
            throw new InParamException();
        }
        List<MenuPO> roleMenuList = menuMapper.findByRole(id);
        List<MenuPO> menuList = menuMapper.findAll(OaConstants.NORMAL_STATE);
        Set<String> menuIdSet = Sets.newHashSet(), parentIdSet = Sets.newHashSet();
        for (MenuPO menu : roleMenuList) {
            menuIdSet.add(menu.getId().toString());
            if (menu.getParentId() != null) {
                findParentMenu(menu.getParentId(), menuList, parentIdSet);
            }
        }
        Map<String, Set<String>> map = Maps.newHashMap();
        map.put("menuIdSet", menuIdSet);
        map.put("parentIdSet", parentIdSet);
        return new CommonResultVo<>().success(map);
    }

    @Override
    @Transactional
    public CommonResultVo menuAuthorize(Integer id, Set<Integer> menuIdSet) {
        if (id == null) {
            throw new InParamException();
        }
        RoleMenu roleMenu = new RoleMenu();
        roleMenu.setRoleId(id);
        roleMenuMapper.delete(roleMenu);
        if (!CollectionUtils.isEmpty(menuIdSet)) {
            // List<RoleMenu> recordList = Lists.newArrayList();
            Date now = new Date();
            Integer userId = LoginInfoDTO.get().getId();
            for (Integer menuId : menuIdSet) {
                roleMenu = new RoleMenu();
                roleMenu.setRoleId(id);
                roleMenu.setMenuId(menuId);
                roleMenu.setCreateTime(now);
                roleMenu.setCreateUserId(userId);
                roleMenuMapper.insert(roleMenu);
                // recordList.add(roleMenu);
            }
            // roleMenuMapper.insertList(recordList);
        }
        return new CommonResultVo().successMsg("角色所属菜单调整成功");
    }

    private void findParentMenu(Integer parentId, List<MenuPO> menuList, Set<String> parentIdSet) {
        for (MenuPO menu : menuList) {
            if (Objects.equals(parentId, menu.getId())) {
                parentIdSet.add(menu.getId().toString());
                if (menu.getParentId() != null) {
                    findParentMenu(menu.getParentId(), menuList, parentIdSet);
                }
            }
        }
    }

    private int updateState(Integer id, Integer state) {
        RolePO role = new RolePO();
        role.setId(id);
        role.setUpdateTime(new Date());
        role.setUpdateUserId(LoginInfoDTO.get().getId());
        role.setState(state);
        return roleMapper.updateByPrimaryKeySelective(role);
    }
}
