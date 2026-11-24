package app.lyra;

import app.lyra.shared.domain.UserId;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiTestConfiguration {

    static {
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(UserId.class);
    }
}
