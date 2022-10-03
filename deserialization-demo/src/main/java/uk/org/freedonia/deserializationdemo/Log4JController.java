package uk.org.freedonia.deserializationdemo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class Log4JController {

    private static final Logger logger = LogManager.getLogger(Log4JController.class);

    @GetMapping("/log")
    public void logMessage(@RequestParam String msg)  {
        logger.info(msg);
    }


}
