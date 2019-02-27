package org.gbif.vocabulary;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(SpringConfig.class)
public class TestApplication {}
