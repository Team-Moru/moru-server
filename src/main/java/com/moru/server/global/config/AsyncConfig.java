package com.moru.server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {


    /**
     * TTS 작업 전용 스레드풀.
     *
     * <p>이걸 정의하지 않으면 Spring 이 기본 실행기를 쓰는데, 기본값은 요청이 올 때마다
     * 스레드를 새로 만든다(상한 없음). 트래픽이 몰리면 스레드가 무한정 늘어나 서버가 죽는다.
     * 그래서 개수를 정해둔 풀을 직접 만들어 쓴다.
     */
    @Bean(name = "ttsExecutor")
    public Executor ttsExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);    // 평소 유지할 스레드 수
        executor.setMaxPoolSize(4);     // 큐가 가득 찼을 때까지 늘릴 수 있는 최대치
        executor.setQueueCapacity(50);  // 대기열. 여기가 차야 비로소 스레드를 더 만든다

        // 로그에서 "요청 스레드"와 "작업 스레드"를 눈으로 구분하기 위한 이름표.
        // 톰캣 요청 스레드는 http-nio-... 로 찍히고, 이쪽은 tts-1, tts-2 로 찍힌다.
        executor.setThreadNamePrefix("tts-");

        executor.initialize();
        return executor;
    }

}
