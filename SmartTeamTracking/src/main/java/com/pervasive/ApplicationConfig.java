package com.pervasive;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.neo4j.config.EnableNeo4jRepositories;
import org.springframework.data.neo4j.config.Neo4jConfiguration;

import com.pervasive.util.FacebookUtils;


@Configuration
@EnableNeo4jRepositories
class ApplicationConfig extends Neo4jConfiguration{
	
	public ApplicationConfig() {
		setBasePackage("com.pervasive");
	}

	@Bean
	GraphDatabaseService graphDatabaseService() {
		return new GraphDatabaseFactory().newEmbeddedDatabase("embeddedNeo4j.db");
	}
	
	@Bean
	FacebookUtils getFacebookUtils(){
		return new FacebookUtils();
	}
}

