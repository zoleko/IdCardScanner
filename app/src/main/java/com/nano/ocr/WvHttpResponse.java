package com.nano.ocr;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class WvHttpResponse {

	private int code;
	 private Throwable  error;
	 private String response;
	 private Boolean is200ok;
	 private String errorMessage;

	 
	 
	 
	 
	
	public WvHttpResponse(int code, Throwable error, String response, Boolean is200ok, String errorMessage) {
		super();
		this.code = code;
		this.error = error;
		this.response = response;
		this.is200ok = is200ok;
		this.errorMessage = errorMessage;
	}
	
	
	public Boolean is200ok() {
		return is200ok;
	}


	public void setIs200ok(Boolean is200ok) {
		this.is200ok = is200ok;
	}


	public String getErrorMessage() {
		return errorMessage;
	}
	public void setErrorMessage(String errorMessage) {
		
		String s="null";
		try {
 			
 			Throwable  e=this.getError();
 			if(e!=null){
 			  Writer writer = new StringWriter();
 			  PrintWriter printWriter = new PrintWriter(writer);
 			  e.printStackTrace(printWriter);
 			  s = writer.toString();
 			}


 		} catch (Exception e) {
          e.printStackTrace();
 		}
		
		this.errorMessage = "code="+getCode()+"|"+errorMessage+"|"+s;
	}
	
	public int getCode() {
		return code;
	}
	public void setCode(int code) {
		this.code = code;
	}
	public Throwable getError() {
		return error;
	}
	public void setError(Throwable error) {
		this.error = error;
	}
	public String getResponse() {
		return response;
	}
	public void setResponse(String response) {
		this.response = response;
	}


	@Override
	public String toString() {
		return "WvWvHttpResponse [code=" + code + ", error=" + error + ", response=" + response + ", is200ok=" + is200ok
				+ ", errorMessage=" + errorMessage + "]";
	}
	 
	 
	 
	
	 
}
