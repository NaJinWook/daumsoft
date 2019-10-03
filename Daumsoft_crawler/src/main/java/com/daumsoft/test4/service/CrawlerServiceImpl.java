package com.daumsoft.test4.service;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import com.daumsoft.test4.model.dao.CrawlerDAO;
import com.daumsoft.test4.model.dto.CrawlerDTO;

@Service
public class CrawlerServiceImpl implements CrawlerService {
	@Inject
	CrawlerDAO crawlerDao;

	public static String page(int num) {
		return "page=" + num;
	}
	
	@Override
	public int count() throws Exception {
		int count = crawlerDao.count();
		int loop = (int) Math.ceil(count * 1.0 / 15);
		return loop;
	}
	
	@Override
	public String getTime() throws Exception {
		String weekAgo = null;
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd");
		cal.add(Calendar.DATE, -7);
		weekAgo = fm.format(cal.getTime());
		return weekAgo;
	}

	@Override
	public void getData(String categoryURL) throws Exception {
		String URL = "https://www.insight.co.kr"+categoryURL;
		String info = null; // 전체 목록 페이지에서 작성자 · 날짜 변수
		int info_length = 0; // 전체 목록 페이지에서 작성자 · 날짜 변수 길이 구해서 substring 하기 위해
		
		//String today = null; // 현재 날짜
		String weekAgo = null; // 일주일 전
		boolean endLine = false;
		CrawlerDTO dto = null;
		List<CrawlerDTO> list = null; // 아무것도 없는 상태에서 전체 불러오기
		
		String idx = null; // 고유 번호
		int type = 1; // 1은 인사이트를 의미함
		String category = null;
		String title = null;
		String content = null;
		String writer = null;
		String email = null;
		String detailURL = null; // 글에 대한 세부 내용을 뽑아오기 위한 URL
		String[] detailURL_split = null; // 글에 대한 세부 내용을 뽑아오기 위한 URL에서 고유 번호를 가져오기 위해
		String regDate = null; // 작성일
		//idx, type, category, title, content, writer, email, detailURL, regDate)

		try {
			list = new ArrayList<CrawlerDTO>();
			for (int i = 1; i < 1000; i++) {
				weekAgo = getTime(); // 일주일 전
				
				Document list_doc = Jsoup.connect(URL + page(i)).get(); // URL로 부터 Document를 가져옴

				Elements subject_elements = list_doc.select(".nav .nav-ul > li > a[title='생활일반']");
				category = subject_elements.text();
				if(category.equals("생활일반")) {
					category = "1";
				}
				
				System.out.println("주제어 : " + category);
				
				Elements list_elements = list_doc.select(".section-list li");
				for (Element list_data : list_elements) {
					// 전체 목록 페이지에서 작성자 · 날짜 출력
					// System.out.println(list_data.select(".section-list-article-byline").text());
					info = list_data.select(".section-list-article-byline").text();
					info_length = info.length();
					regDate = info.substring(info_length - 19, info_length);
					detailURL = list_data.select("a").attr("href");
					detailURL_split = detailURL.split("/");
					idx = detailURL_split[4];

					int compare = weekAgo.compareTo(regDate);
					if (compare > 0) {
						System.out.println("프로그램 종료");
						endLine = true;
						break;
					}
					
					// 세부 URL
					// System.out.println(detailURL);
					Document detail_doc = Jsoup.connect(detailURL).get();
					Elements detail_data = detail_doc.select(".content");
					
					title = detail_data.select(".news-header h1").text();
					content = detail_data.select(".news-article-memo > p").text();
					writer = detail_data.select(".news-byline-writer").text();
					email = detail_data.select(".news-byline-mail").text();
					System.out.println("weekAgo 날짜는 " + weekAgo);
					System.out.println("고유번호 : " + idx);
					System.out.println("사이트 종류 : " + type);
					System.out.println("카테고리 : " + category);
					System.out.println("제목 : " + title);
					System.out.println("내용 : " +  content);
					System.out.println("작성자 : " + writer);
					System.out.println("메일 : " + email);
					System.out.println("URL : " + detailURL);
					System.out.println("작성일 : " + regDate);
					System.out.println("======================================================");
					dto = new CrawlerDTO(idx, type, Integer.parseInt(category), title, content, writer, email, detailURL, regDate);
					list.add(dto);
				}
				if(endLine) {
					break;
				}
			}
			crawlerDao.getData(list);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addData(String categoryURL) throws Exception {
		String URL = "https://www.insight.co.kr"+categoryURL;
		int loop = count(); // 페이지 반복 범위 지정
		String info = null; // 전체 목록 페이지에서 작성자 · 날짜 변수
		int info_length = 0; // 전체 목록 페이지에서 작성자 · 날짜 변수 길이 구해서 substring 하기 위해

		//String today = null; // 현재 날짜
		String weekAgo = null; // 일주일 전
		boolean endLine = false;
		boolean switchList = false;
		CrawlerDTO dto = null;
		List<CrawlerDTO> add_list = null; // 가지고 있지 않은 최신 데이터 등록하는 리스트
		List<CrawlerDTO> update_list = null; // 가지고 있는 데이터 업데이트 하는 리스트
		
		String idx = null; // 고유 번호
		String top_idx = null; // 테이블 번호가 1인 idx 가져오기 위함
		int type = 1; // 1은 인사이트를 의미함
		String category = null;
		String title = null;
		String content = null;
		String writer = null;
		String email = null;
		String detailURL = null; // 글에 대한 세부 내용을 뽑아오기 위한 URL
		String[] detailURL_split = null; // 글에 대한 세부 내용을 뽑아오기 위한 URL에서 고유 번호를 가져오기 위해
		String regDate = null; // 작성일
		//idx, type, category, title, content, writer, email, detailURL, regDate

		try {
			add_list = new ArrayList<CrawlerDTO>();
			update_list = new ArrayList<CrawlerDTO>();
			for (int i = 1; i <= loop; i++) {
				weekAgo = getTime(); // ~~까지 업데이트
				top_idx = crawlerDao.top_idx(); // 테이블 번호가 1인 최상위 idx 구하기 위함
				
				Document list_doc = Jsoup.connect(URL + page(i)).get(); // URL로 부터 Document를 가져옴

				Elements subject_elements = list_doc.select(".nav .nav-ul > li > a[title='생활일반']");
				category = subject_elements.text();
				if(category.equals("생활일반")) {
					category = "1";
				}
				//System.out.println("주제어 : " + category);
				
				Elements list_elements = list_doc.select(".section-list li");
				for (Element list_data : list_elements) {
					// 전체 목록 페이지에서 작성자 · 날짜 출력
					info = list_data.select(".section-list-article-byline").text();
					info_length = info.length();
					regDate = info.substring(info_length - 19, info_length);
					detailURL = list_data.select("a").attr("href");
					detailURL_split = detailURL.split("/");
					idx = detailURL_split[4];
					
					// 세부 URL
					Document detail_doc = Jsoup.connect(detailURL).get();
					Elements detail_data = detail_doc.select(".content");
					
					title = detail_data.select(".news-header h1").text();
					content = detail_data.select(".news-article-memo > p").text();
					writer = detail_data.select(".news-byline-writer").text();
					email = detail_data.select(".news-byline-mail").text();
					
					int compare = weekAgo.compareTo(regDate);
					if (compare > 0) {
						System.out.println("프로그램 종료");
						endLine = true;
						break;
					}
					if(!idx.equals(top_idx) && !switchList) {
						
						//System.out.println("고유번호 : " + idx);
						//System.out.println("사이트 종류 : " + type);
						//System.out.println("카테고리 : " + category);
						//System.out.println("제목 : " + title);
						//System.out.println("내용 : " +  content);
						//System.out.println("작성자 : " + writer);
						//System.out.println("메일 : " + email);
						//System.out.println("URL : " + detailURL);
						//System.out.println("작성일 : " + regDate);
						//System.out.println("======================================================");
						dto = new CrawlerDTO(idx, type, Integer.parseInt(category), title, content, writer, email, detailURL, regDate);
						add_list.add(dto);
					} else {
						switchList = true;
						dto = new CrawlerDTO(idx, type, Integer.parseInt(category), title, content, writer, email, detailURL, regDate);
						update_list.add(dto);
					}
				}
				if(endLine) {
					break;
				}
			}
			System.out.println("추가 해야 될 게시글 수 : " + add_list.size() + "이고 수정되어야 될 게시글 수는 " + update_list.size());
			//crawlerDao.update_list(update_list);
			//crawlerDao.add_list(add_list);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
