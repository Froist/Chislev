import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.jsoup.Connection.Response;
import org.jsoup.Connection.Method;

import java.io.IOException;
import java.io.*;

/* To compile, grab a jsoup jar file, say its saved with jsoup.jar, then( on Windows ) run:
 * javac -cp ARC filename.java
 * java -cp ARC filename
 * 
 * where ARC on
 * Windows = .;jsoup.jar
 * Linux = '.:jsoup.jar'
*/
 
public class scratch 
{
	static Elements H2Tag( Document doc )
	{
		Elements h2_element = doc.getElementsByTag( "h2" );
		if( h2_element == null || h2_element.size() == 0 ){
			System.out.println( "Unable to find <h2> tag" );
			return null;
		}
		
		if( h2_element.get( 0 ).attr( "class" ) == null ){
			return null;
		}
		return h2_element;
	}
	
	static boolean WriteDataToFile( QuestionInfo questionInfo, int index ) throws IOException
	{
		File file = new File( "./quest/" + String.valueOf( index ) + ".txt" );
		
		FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter( file );
			fileWriter.write( "%%\n" );
			fileWriter.write( "$$ Question\n" );
			fileWriter.write( "According to the C++11 standard, what is output of the following code?\n" );
			fileWriter.write( "\n");
			fileWriter.write( "$$ Code\n" );
			fileWriter.write( questionInfo.getQuestionCode() + "\n" );
			fileWriter.write( "\n" );
			fileWriter.write( "$$ Level\n" );
			fileWriter.write( questionInfo.getQuestionLevel() + "\n" );
			fileWriter.write( "\n" );
			fileWriter.write( "$$ Hint\n" );
			fileWriter.write( questionInfo.getQuestionHint() + "\n" );
			fileWriter.write( "\n" );
			fileWriter.write( "$$ Explanation\n" );
			fileWriter.write( questionInfo.getQuestionExplanation() + "\n" );
			fileWriter.write( "\n" );
			
			fileWriter.write( "$$ Answer\n" );
			fileWriter.write( questionInfo.getQuestionAnswer() + "\n" );
			fileWriter.write( "\n" );	
		} catch( IOException except ){
			System.out.println( except.getMessage() );
			return false;
		} finally {
			fileWriter.close();
			return true;
		}
	}
	
	public static void StartGrabbing( final int index )
	{
		System.out.println( "Grabbing question: " + index );
		
		final String hintUrl = "http://cppquiz.org/quiz/question/" + String.valueOf( index ) + "?show_hint=1",
			answerUrl = "http://cppquiz.org/quiz/question/" + String.valueOf( index ) +
			"?result=OK&answer=WHATEVER&did_answer=Answer",
			giveupUrl = "http://cppquiz.org/quiz/giveup/" + String.valueOf( index );
		
		Document doc = null;
		Elements h2_element_tag = null;
		try {
			doc = Jsoup.connect( hintUrl ).timeout( 10000 ).get();
			h2_element_tag = H2Tag( doc );
		} catch( Exception except ) {
			System.out.println( "Question index " + index + " doesn't exist. " +except.getMessage() );
			return;
		}
		if( h2_element_tag == null || doc == null )
		{
			System.out.println( "doc/h2_element_tag is NULL" );
			return;
		}
		
		Elements codeTag = doc.getElementsByAttributeValue( "class", "sh_cpp" ),
				 hintTag = doc.getElementsByAttributeValue( "class", "hint" ),
				 difficultyTag = doc.getElementsByAttributeValue( "class", "about_the_question" );
		QuestionInfo questionInfo = new QuestionInfo();
		if( codeTag.size() > 0 ){
			List<TextNode> codeTexts = codeTag.get( 0 ).textNodes();
			if( codeTexts != null && codeTexts.size() > 0 ){
				StringBuilder stringBuilder = new StringBuilder();
				for( TextNode sb : codeTexts ){
					stringBuilder.append( sb.text() );
				}
				questionInfo.setQuestionCode( stringBuilder.toString() );
			}
		}
		if( difficultyTag != null && difficultyTag.size() != 0 ){
			Element spanElement = difficultyTag.get( 0 );
			if( spanElement.tagName().equals( "span" ) ){
				Elements number = spanElement.getElementsByTag( "a" ),
						difficult = spanElement.getElementsByTag( "img" );
				if( number != null && number.size() != 0 ){
					questionInfo.setQuestionNumber( number.get( 0 ).text() );
				}
				if( difficult != null && difficult.size() != 0 ){
					questionInfo.setDifficultyLevel( difficult.get( 0 ).attr( "src" ) );
				}
			}
		}
		if( hintTag != null ){
			for( Element elem : hintTag ){
				if( elem.tagName().equals( "div" ) ){
					questionInfo.setQuestionHint( elem.text() );
					break;
				}
			}
		}
		Response response = null;
		try {
			response = Jsoup.connect( answerUrl ).userAgent( "Mozilla" ).method( Method.GET ).timeout( 10000 ).execute();
		} catch( Exception exc ){
			System.out.println( "Unable to get answers" );
			return;
		}
		
		Map<String, String> sessionMap = response.cookies();
		Set<String> f = sessionMap.keySet();
		String[] keys = f.toArray( new String[f.size()] );
		
		final String key = keys[0], sessionID = sessionMap.get( key );
		for( int i = 0; i != 3; ++i ){
			try {
				Jsoup.connect( answerUrl ).cookie( key, sessionID ).timeout( 7000 ).get();
			} catch( Exception e ){
				System.out.println( e.getMessage() );
				return;
			}
		}
		
		Document answerDoc = null;
		try {
			answerDoc = Jsoup.connect( giveupUrl ).cookie( key, sessionID ).userAgent( "Mozilla" ).method( Method.GET ).
			timeout( 7000 ).get();
		} catch( Exception ex ){
			System.out.println( "Could not get GiveUp to load. " + ex.getMessage() );
			return;
		}
		Elements divTag = answerDoc.getElementsByAttributeValue( "id", "main_col" );
		Elements h3Tag = answerDoc.getElementsByTag( "h3" );
		
		if( h3Tag != null && h3Tag.size() != 0 ){
			for( int i = 0; i != h3Tag.size(); ++i ){
				Element elem = h3Tag.get( i );
				if( elem.text().compareToIgnoreCase( "answer" ) == 0 ){
					if( i < h3Tag.size() - 1 ){
						Element nextElem = h3Tag.get( i + 1 );
						Element tempElem = elem.nextElementSibling();
						StringBuilder myCorrectBuilder = new StringBuilder();
						while( tempElem != nextElem ){
							myCorrectBuilder.append( tempElem.text() + "\n" );
							tempElem = tempElem.nextElementSibling();
						}
						questionInfo.setQuestionAnswer( myCorrectBuilder.toString() );
					}
					break;
				}
			}
		}
		if( divTag != null && divTag.size() != 0 ){
			Elements divChildTag = divTag.get( 0 ).getElementsByTag( "div" );
			questionInfo.setQuestionExplanation( divChildTag.get( 1 ).text() ); 
		}
		try {
			WriteDataToFile( questionInfo, index );
		} catch( IOException io ){
			System.out.println( io.getMessage() );
		}
	}
	
	public static void main ( String args[] )
	{
		ExecutorService executor = Executors.newFixedThreadPool( 5 );
		for( int i = 0; i != 160; ++i ){
			final int x = i + 1;
			executor.execute( ()-> StartGrabbing( x ) );
		}
		executor.shutdown();
	}
	
	private static class QuestionInfo
	{
		private String difficultyLevel;
		private String questionNumber;
		private String questionCode;
		private String questionHint;
		private String questionAnswer;
		private String questionExplanation;
		
		public QuestionInfo(){ }
		public void setDifficultyLevel( final String level ) { difficultyLevel = level; }
		public void setQuestionNumber( final String number ) { questionNumber = number; }
		public void setQuestionCode( final String code ) { questionCode = code; }
		public void setQuestionHint( final String hint ) { questionHint = hint; }		
		public void setQuestionAnswer( final String answer ) { questionAnswer = answer; }
		public void setQuestionExplanation( final String explanation ) { questionExplanation = explanation; }
		
		public String getQuestionLevel(){ return difficultyLevel; }
		public String getQuestionCode() { return questionCode; }
		public String getQuestionHint() { return questionHint; }
		public String getQuestionAnswer() { return questionAnswer; }
		public String getQuestionExplanation() { return questionExplanation; }
	}
}
