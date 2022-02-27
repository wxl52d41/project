package com.example.demo.controller;

import com.example.demo.entity.Student;
import com.example.demo.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;

/**
 * @author xlwang55
 * @date 2022/2/24 14:43
 */

@RestController
@RequestMapping("/student")
public class StudentController {
    @Autowired
    private StudentService studentService;

    //http://localhost:8079/student/insert
    @PostMapping("/insert")
    public void insert() {
        Student student = new Student();
        student.setStudentName("张三");
        student.setAge(18);
        student.setSex(1);
        student.setIsDelete(0);
//        studentService.insertStudent(student);
         studentService.save(student);
    }

    //http://localhost:8079/student/insertBatch
    @PostMapping("/insertBatch")
    public void insertBatch() {
        Student student = new Student();
        student.setStudentName("张三");
        student.setAge(18);
        student.setSex(1);
        student.setIsDelete(0);
        Student student2 = new Student();
        student2.setStudentName("李四");
        student2.setAge(19);
        student2.setSex(1);
        student2.setIsDelete(0);
        ArrayList<Student> students = new ArrayList<>();
        students.add(student);
        students.add(student2);
        studentService.saveBatch(students);
    }

    @PutMapping("/update")
    public void update(@RequestBody Student student) {
        studentService.updateById(student);
    }

    @DeleteMapping("/delete")
    public void delete(@RequestBody Student student) {
        studentService.removeById(student);
    }
}
