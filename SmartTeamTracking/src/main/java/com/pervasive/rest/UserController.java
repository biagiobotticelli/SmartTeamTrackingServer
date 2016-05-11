package com.pervasive.rest;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pervasive.model.Beacon;
import com.pervasive.model.Group;
import com.pervasive.model.User;
import com.pervasive.repository.BeaconRepository;
import com.pervasive.repository.GroupRepository;
import com.pervasive.repository.UserRepository;
import com.pervasive.util.FacebookUtils;

@RestController
public class UserController {

	@Autowired
	private ApplicationContext context;
    
	
    @RequestMapping("/user")
    public User authOrSignupUser(@RequestParam(value="token", defaultValue="null") String token,
    			                 @RequestParam(value="facebookId", defaultValue="null") String facebookId,
    						     @RequestParam(value="name", defaultValue="null") String name,
						         @RequestParam(value="surname", defaultValue="null") String surname,
						         @RequestParam(value="email", defaultValue="null") String email){
    	
    	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);    	
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
        FacebookUtils facebookUtils = (FacebookUtils) context.getBean(FacebookUtils.class);
        
        User userFromNeo;
        
    	//Check if this user already signed up 
        Transaction tx = graphDatabaseService.beginTx();
		try{
			userFromNeo = userRepository.findByFacebookId(facebookId);
			tx.success();
		}
		finally{
			tx.close();
		}
		
		if( userFromNeo == null)
			 return facebookUtils.signupUser(token, name, surname, email);
		else return facebookUtils.authUser(userFromNeo, token);
    }
    
    
    //Returns true if correctly executed, if can find either group or beacon returns false 
    @RequestMapping(method = RequestMethod.POST,value="/user/{userId}/{beaconIdentifier}")
    public boolean addInRange(@PathVariable Long userId, @PathVariable Long beaconIdentifier){
    	
    	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
    	BeaconRepository beaconRepository = (BeaconRepository) context.getBean(BeaconRepository.class);
    	
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
    	Transaction tx = graphDatabaseService.beginTx();
		try{
			User userFromNeo = userRepository.findById(userId);
			Beacon beaconFromNeo = beaconRepository.findByBeaconIdentifier(beaconIdentifier);
			
			if(userFromNeo == null || beaconRepository == null){
				tx.success();
				tx.close();
				return false;
			}
			
			//This also covers the case in which the beacon is null. 
			userFromNeo.setBeacon(beaconFromNeo);
			userRepository.save(userFromNeo);
			tx.success();
        }
		finally{
			tx.close();
		}
		return true;
    }
    
  
    @RequestMapping("/user/{userId}/groups")
    public List<Group> getGroupsOfUsers(@PathVariable Long userId){
    	
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
        
        List<Group> groupList = new LinkedList<>();

    	Transaction tx = graphDatabaseService.beginTx();
    	try{
			Iterable<Group> iterableGroup = groupRepository.getGroupsforUser(userId);
			Iterator<Group> it = iterableGroup.iterator();
			while (it.hasNext()){
				Group g = it.next();
				g.invalidContains();
				g.invalidPending();
				groupList.add(g);
			}
			tx.success();	
        }
		finally{
			tx.close();
		}
		return groupList;
    }
  

    @RequestMapping("/user/{userId}/pending")
    public List<Group> getPendingGroupsOfUsers(@PathVariable Long userId){
    	
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
        
        List<Group> groupList = new LinkedList<>();

    	Transaction tx = graphDatabaseService.beginTx();
    	try{
			Iterable<Group> iterableGroup = groupRepository.getPendingGroupsForUser(userId);
			Iterator<Group> it = iterableGroup.iterator();
			while (it.hasNext()){
				Group g = it.next();
				g.invalidContains();
				g.invalidPending();
				groupList.add(g);
			}
			tx.success();	
        }
		finally{
			tx.close();
		}
		return groupList;
    }
    
    
    @RequestMapping(method = RequestMethod.POST,value="/user/{userId}/")
    public boolean updateUserGPSCoordinates(@PathVariable Long userId,
    									   @RequestParam(value="lat", defaultValue="null") Double latitude, 
    									   @RequestParam(value="lon", defaultValue="null") Double longitude){
    	
    	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
        
        Transaction tx = graphDatabaseService.beginTx();
		try{
			User userFromNeo = userRepository.findById(userId);
			if(userFromNeo == null){
				tx.success();
				tx.close();
				return false;
			}
			
			userFromNeo.setLatGPS(latitude);
			userFromNeo.setLonGPS(longitude);
			userRepository.save(userFromNeo);
			
			tx.success();
        	}
		
		finally{
			tx.close();
		}
		return true;
    }
    
    
    @RequestMapping(method = RequestMethod.POST,value="/user/{userId}/{groupId}/accept")
    public Group addContains(@PathVariable Long userId, @PathVariable Long groupId){
    	
    	Group groupFromNeo;
    	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
        
    	Transaction tx = graphDatabaseService.beginTx();
		try{
			User userFromNeo = userRepository.findById(userId);
			groupFromNeo = groupRepository.findById(groupId);
			if( userFromNeo == null || groupFromNeo == null){
				tx.success();
				tx.close();
				return null;
			}
			
			groupFromNeo.removeUserPending(userFromNeo);
			groupFromNeo.addUser(userFromNeo);
			groupRepository.save(groupFromNeo);
			tx.success();				
        }
		finally{
			tx.close();
		}
		return groupFromNeo;
    }
    
    
    @RequestMapping(method = RequestMethod.POST,value="/user/{userId}/{groupId}/refuse")
    public boolean removePending(@PathVariable Long userId, @PathVariable Long groupId){
    	
    	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);
        
    	Transaction tx = graphDatabaseService.beginTx();
		try{
			User userFromNeo = userRepository.findById(userId);
			Group groupFromNeo = groupRepository.findById(groupId);
		    if(userFromNeo == null || groupFromNeo == null){
		    	tx.success();
		    	tx.close();
		    	return false;
		    }
	
			groupFromNeo.removeUserPending(userFromNeo);
		    groupRepository.save(groupFromNeo);
		    tx.success();
        }
		finally{
			tx.close();
		}
		return true;
    }
   
  //Returns null if can't find User. This is a demo rest call, to not be used in production. 
    
  	/*@RequestMapping("/user")
      public User findUser(@RequestParam(value="name", defaultValue="null") String name) {
      	
      	User userFromNeo;
      	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
          GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);

      	Transaction tx = graphDatabaseService.beginTx();
  		try{
  			userFromNeo = userRepository.findByName(name);
  			tx.success();
  		
  			if(userFromNeo == null) {
  				tx.close();
  				return null;
  			}				
          }
  		finally{
  			tx.close();
  		}
  		return userFromNeo;
      }*/ 
   
    /* This API shouldn't be needed! Can invite from group! 
    @RequestMapping(method = RequestMethod.POST,value="/user/{email}/{groupId}/invite")
    public Group addPending(@PathVariable String email, @PathVariable Long groupId){
    	
    	Group groupFromNeo;
    	UserRepository userRepository = (UserRepository) context.getBean(UserRepository.class);
    	GroupRepository groupRepository = (GroupRepository) context.getBean(GroupRepository.class);
        GraphDatabaseService graphDatabaseService = (GraphDatabaseService) context.getBean(GraphDatabaseService.class);

    	Transaction tx = graphDatabaseService.beginTx();
		try{
			User UserFromNeo = userRepository.findByEmail(email);
			groupFromNeo = groupRepository.findById(groupId);
			groupFromNeo.addUserPending(UserFromNeo);
			groupRepository.save(groupFromNeo);
			tx.success();
        }
		finally{
			tx.close();
		}
		return groupFromNeo;
    }
    */
}
