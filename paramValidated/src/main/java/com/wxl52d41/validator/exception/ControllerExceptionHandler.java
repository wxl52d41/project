package com.wxl52d41.validator.exception;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.wxl52d41.validator.vo.Result;
import io.swagger.annotations.ApiModelProperty;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.hibernate.validator.internal.metadata.core.ConstraintHelper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 异常处理器
 *
 * @Date 2019
 */
@RestControllerAdvice
@Slf4j
public class ControllerExceptionHandler {

	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(CustomException.class)
	public Result<?> handleRRException(CustomException e){
		log.error(e.getMessage(), e);
		return Result.error(e.getMessage());
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public Result<?> handlerNoFoundException(Exception e) {
		log.error(e.getMessage(), e);
		return Result.error(404, "路径不存在，请检查路径是否正确");
	}

	@ExceptionHandler(DuplicateKeyException.class)
	public Result<?> handleDuplicateKeyException(DuplicateKeyException e){
		log.error(e.getMessage(), e);
		return Result.error("数据库中已存在该记录,不允许重复添加！");
	}

	@ExceptionHandler({UnauthorizedException.class, AuthorizationException.class})
	public Result<?> handleAuthorizationException(AuthorizationException e){
		log.error(e.getMessage(), e);
		return Result.noauth("没有权限，请联系管理员授权");
	}

	@ExceptionHandler(Exception.class)
	public Result<?> handleException(Exception e){
		log.error(e.getMessage(), e);

		return Result.error(e.getMessage());
	}

	/**
	 * post请求的对象参数校验失败后抛出的异常
	 * @param e
	 * @return
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public Result<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e){

		log.error(e.getMessage(), e);
		return Result.error(HttpStatus.BAD_REQUEST.value(),
				e.getBindingResult().getAllErrors().stream()
						.map(ObjectError::getDefaultMessage)
						.collect(Collectors.joining("; "))
		);
	}

	/**
	 * 单参数校验失败
	 * @param e
	 * @return
	 */
	@ExceptionHandler(ConstraintViolationException.class)
	public Result<?> handleConstraintViolationException(ConstraintViolationException e){

		log.error(e.getMessage(), e);
		return Result.error(HttpStatus.BAD_REQUEST.value(),
				e.getConstraintViolations().stream()
						.map(ConstraintViolation::getMessage)
						.collect(Collectors.joining("; "))
		);
	}

	/**
	 *
	 * et请求的对象参数校验失败后抛出的异常
	 * @param e
	 * @return 
	 */
	@ExceptionHandler(BindException.class)
	public Result<?> handleBindException(BindException e){

		log.error(e.getMessage(), e);
		return Result.error(HttpStatus.BAD_REQUEST.value(),
				e.getAllErrors().stream()
						.map(ObjectError::getDefaultMessage)
						.collect(Collectors.joining("; "))
		);
	}
	
	/**
	 * @Author 政辉
	 * @param e
	 * @return
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public Result<?> HttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e){
		StringBuffer sb = new StringBuffer();
		sb.append("不支持");
		sb.append(e.getMethod());
		sb.append("请求方法，");
		sb.append("支持以下");
		String [] methods = e.getSupportedMethods();
		if(methods!=null){
			for(String str:methods){
				sb.append(str);
				sb.append("、");
			}
		}
		log.error(sb.toString(), e);
		//return Result.error("没有权限，请联系管理员授权");
		return Result.error(405,sb.toString());
	}
	
	 /** 
	  * spring默认上传大小100MB 超出大小捕获异常MaxUploadSizeExceededException 
	  */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
    	log.error(e.getMessage(), e);
        return Result.error("文件大小超出10MB限制, 请压缩或降低文件质量! ");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public Result<?> handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    	log.error(e.getMessage(), e);
        return Result.error("字段太长,超出数据库字段的长度");
    }


}
