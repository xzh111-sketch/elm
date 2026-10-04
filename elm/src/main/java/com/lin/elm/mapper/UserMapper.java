package com.lin.elm.mapper;

import com.lin.elm.po.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface UserMapper {
    @Select("select * from user where userId=#{userId}")
    public User findByUserId(String userId);
    @Select("select delTag from user where userId=#{userId}")
    public int getUserById(String userId);
    @Insert("insert into user(userId,password,userName,userSex,userImg,delTag) values(#{userId},#{password},#{userName},#{userSex},null,1)")
    public int saveUser(User user);
    @Update("update user set userName=#{userName},userSex=#{userSex},userImg=#{userImg} where userId=#{userId}")
    public int updateUser(User user);
    @Update("update user set userImg=#{userImg} where userId=#{userId}")
    public int updateUserImg(User user);
    @Update("update user set password=#{password} where userId=#{userId}")
    public int updatePassword(User user);
}
