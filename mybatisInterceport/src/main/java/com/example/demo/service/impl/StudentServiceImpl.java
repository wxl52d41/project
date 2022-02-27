package com.example.demo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.demo.entity.Student;
import com.example.demo.mapper.StudentMapper;
import com.example.demo.service.StudentService;
import org.springframework.stereotype.Service;

/**
 * @author xlwang55
 * @date 2022/2/24 14:50
 */
@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper,Student> implements StudentService {

}
