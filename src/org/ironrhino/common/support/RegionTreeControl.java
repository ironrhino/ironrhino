package org.ironrhino.common.support;

import org.ironrhino.common.model.Region;
import org.ironrhino.core.service.BaseTreeControl;
import org.ironrhino.core.spring.configuration.ApplicationContextPropertiesConditional;
import org.springframework.stereotype.Component;

@ApplicationContextPropertiesConditional(key = "regionTreeControl.disabled", value = "true", negated = true)
@Component
public class RegionTreeControl extends BaseTreeControl<Region> {

}
