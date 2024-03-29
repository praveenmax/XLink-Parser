package org.indiedev.xlink.tree;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.indiedev.xlink.elements.Arc;
import org.indiedev.xlink.elements.Locator;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/*
 * This is the XLink processor which extracts the tree labelnodes and its info
 * STEPS:(BRIEF)
 * -------------
 * 1.Load the xml file(having xlink tags).
 * 2.Extract all the 'locator' elements and store in 'locatorList'.
 * 3.Extract all the 'arc' elements and store in 'arcList'.
 * 4.Create a relationship b/w locators by reading each 'arc' elements from the 'arcList'.
 * 
 * NOTE:LabelTree is accessed from here only!.
 */
public class LabelTreeProcessor 
{
	//this list contains the locator nodes.Used for validating only.
	private ArrayList<String> locatorList;
	
	//this list contains the 'xlink:to' attrib values.Used for subtraction with 'locatorList' list for 
	//finding the root node.
	private ArrayList<String> arcList_To;
	
	//this list contains the arc nodes.LabelNodes are created from here only.
	private ArrayList<Arc> arcList;
	
	private LabelTree labelTree;
	
	private Locator root_LocatorNode=null;
	
	//xml vars
	private DocumentBuilderFactory db_factory;
	private DocumentBuilder doc_builder;
	private Document document;
	
	/* PRESENTATION LINK ELEMENTS */
	//Root tag
	private final static String ext_PresentationLink="link:presentationLink";
	private final static String ext_PresentationLinkAttrib_role="xlink:role";
		//Locator tag
		private final static String loc_Presentation="link:loc";
		private final static String loc_Presentation_Label="xlink:label";
		//Arc tag
		private final static  String arc_Presentation="link:presentationArc";
		private final static String arc_Presentation_From="xlink:from";
		private final static String arc_Presentation_To="xlink:to";
		private final static String arc_Presentation_Order="order";
	
	/*
		INPUT:an XML file
		OUTPUT:none
		
		Gets the element labels from the XML file and stores in arraylist.
		populates the arcList,locatorList,arcList_To
	 */
	public void loadFile(File xmlFile)
	{
		try
		{
			db_factory=DocumentBuilderFactory.newInstance();
			doc_builder=db_factory.newDocumentBuilder();
			document=doc_builder.parse(xmlFile);
		}
		catch(ParserConfigurationException e)
		{
			System.out.println("Error while initializing document builders!!"+e);
		}
		catch(SAXException e)
		{
			System.out.println("Error while reading XML file!!"+e);
		}
		catch(IOException e)
		{
			System.out.println("Error while reading XML file!!"+e);
		}
		catch(NullPointerException e)
		{
			System.out.println(e);
		}
	}
	
	//RETURNS:NodeList of the input node
	private NodeList getEntireNodeList(String t_node)
	{
		NodeList t_nodeList=document.getElementsByTagName(t_node);
		
		if(t_nodeList.getLength()>0)
			return t_nodeList;
		else
			return null;

	}
	
	/*
	 * Prints the node name of the given NodeList
	 */
	private void printNodeList(NodeList nodeList)
	{
		System.out.println("NodeList Details:LENGTH="+nodeList.getLength());
		System.out.println("----------------");
		for(int i=0;i<nodeList.getLength();i++)
		{
			System.out.println("node["+i+"] : "+nodeList.item(i).getNodeName());
		}		
	}
	
	/*
	 * Extracts nodes of given name from the nodeList.
	 * Returns:Node[] containing req.nodes
	 */
	private List<Node> extractNodes(NodeList nodeList,String nodeToExtract)
	{
		List<Node> list_nodeList=new ArrayList<Node>();
		Node tempNode=null;
		
		System.out.println("Extracting Node of name  :  "+nodeToExtract);
		System.out.println("-----------------------------");
		for(int i=0;i<nodeList.getLength();i++)
		{
			tempNode=nodeList.item(i);
			if(tempNode.getNodeName().equals(nodeToExtract))
			{
				//add to List<Node>
				list_nodeList.add(tempNode);
			}
		}
		return list_nodeList;
	}
	
	/*
	 * Extracts a specific attribute values from the given nodeList.
	 * Return:String[] containing attribute values
	 */
	private String[] extractNodesAttributes( List<Node> list_nodeList,String attribName)
	{
		Iterator<Node> itr=list_nodeList.iterator();
		String[] attribValues=new String[list_nodeList.size()];
		int count=0;
		
		while(itr.hasNext())
		{
			attribValues[count]=itr.next().getAttributes().getNamedItem(attribName).getNodeValue();
			count++;          
		}
		System.out.println("Attribute values Count: "+attribValues.length);
			
		return attribValues;
	}
	
	//should populate locators within a single 'presentationLink" node only!
	//Extracts all locator nodes under the given 'role' node.
	private void populateLocatorList(Node t_roleNode)
	{
		System.out.println("[POPULATE_LocatorList]:Extracting Locators");
		if(t_roleNode.hasChildNodes())
		{
			locatorList=new ArrayList<String>();
			
			//getting the child nodes of this particulat presentation node
			NodeList nodeList=t_roleNode.getChildNodes();
			//System.out.println("Received Locators count:"+nodeList.getLength());
			//this.printNodeList(nodeList);

			//extracting only locator(link:loc) nodes from the nodeList
			List<Node> list_LocatorNodeList=this.extractNodes(nodeList, loc_Presentation);
			
			//extracting Locator attribute:labels
			String[] locatorLabels=this.extractNodesAttributes(list_LocatorNodeList, loc_Presentation_Label);
			
			//adding to locatorList(ArrayList)
			Collections.addAll(locatorList, locatorLabels);
	
			System.out.println("[POPULATE_LocatorList]:Completed.List size:"+locatorList.size());
		}
		else
		{
			System.out.println("ERROR[POPULATE_LocatorList]:Given Role Node has no childs! ");
		}
		
	}//populateList
	
	
	//should populate arcs within a single 'presentationLink" node only!
	//Extracts all arc nodes under the given 'role' node.
	private void populateArcList(Node t_roleNode)
	{
		String[] Arc_From_Values=null;
		String[] Arc_To_Values=null;
		String[] orders=null;
		
		System.out.println("[POPULATE_ArcList]:Extracting arcs");
		if(t_roleNode.hasChildNodes())
		{
			//initializing arclist and arcTo list
			arcList_To=new ArrayList<String>();
			arcList=new ArrayList<Arc>();
			
			//getting the child nodes of this particulat presentation node
			NodeList nodeList=t_roleNode.getChildNodes();

			//extracting only arc(link:presentationArc) nodes from the nodeList
			List<Node> list_ArcNodeList=this.extractNodes(nodeList, LabelTreeProcessor.arc_Presentation);
	
				//extracting xlink:from, xlink:to and order attribute from the list_ArcNodeList..
				Arc_From_Values=this.extractNodesAttributes(list_ArcNodeList, arc_Presentation_From);
				Arc_To_Values=this.extractNodesAttributes(list_ArcNodeList, arc_Presentation_To );
				orders=this.extractNodesAttributes(list_ArcNodeList, arc_Presentation_Order );
				
				System.out.println("Extracted Arc Info:");
				System.out.println("  COUNTS::  Arc_From:"+Arc_From_Values.length);
				System.out.println("            Arc_From:"+Arc_To_Values.length);
				System.out.println("            Order:"+orders.length);
			
			//creating 'arc' object and inserting 'from' , 'to' ,'order' values.
			for(int i=0;i<list_ArcNodeList.size();i++)
			{
				arcList.add(new Arc(Arc_From_Values[i], Arc_To_Values[i], Float.parseFloat(orders[i])) );
			
				//also populate 'arcList_To' arraylist
				arcList_To.add(Arc_To_Values[i]);
			
			}//outer-for
			
			System.out.println("[POPULATE_ArcList]:Completed.List size:"+arcList.size());
			System.out.println("[POPULATE_Arc_To_List]:Completed.List size:"+arcList_To.size());
		}
		else
		{
			System.out.println("ERROR[POPULATE_ArcList]:Given Role Node has no childs! ");
		}
	}
	
	/*
	 * Extracts the xlink(locators,arcs) node information under the given 'presentationrole'.
	 * Inner process:
	 * --------------
	 * 1.populates LocatorList.
	 * 2.populates ArcList(also arcTo list).
	 * 3.Finds the rootNode of this particular presentation.
	 */
	public void processPresentationNode(String t_role)
	{
		//getting all 'link:presentationLink' nodes
		NodeList nodeList_PresentationLink=getEntireNodeList(ext_PresentationLink);
		
		if(nodeList_PresentationLink!=null)
		{
			String roleNodeName=null;
			Node roleNode=null;
		
			//checking the 'role' attribute of all the retrieved 'link:presentation' nodes
			for(int i=0;i<nodeList_PresentationLink.getLength();i++)
			{
				//storing the rolenode 
				roleNode=nodeList_PresentationLink.item(i);
				
				//getting the value of 'xlink:role' attr
				roleNodeName=nodeList_PresentationLink.item(i).getAttributes().getNamedItem(ext_PresentationLinkAttrib_role).getNodeValue();
				
				//System.out.println("Debug:"+roleNodeName);
				//checking whether 't_role' exists
				if(roleNodeName.equals(t_role))
				{
					System.out.println("ROLE: "+t_role+" exists!.Populating \'Locator\' and \'Arc\' List");
					
					//passing the roleNode itself .
					populateLocatorList(roleNode);
					populateArcList(roleNode);
					
					//find the root node
					root_LocatorNode=getRootNode();
					
					break;
				}//if
			}//for
		}
		else
		{
			System.out.println("[ERROR] : Node \'"+ext_PresentationLink+"\' not found!");
		}
	}
	
	/*
	 * Finds the root node in the XLink document.
	 * 
	 * 
	 */
	public Locator getRootNode() throws NullPointerException 
	{
		ArrayList<String> tempArrayList=new ArrayList<String>(locatorList);
		
		//removes all the elements except one which should be the ROOTNODE
		if(arcList_To.size()!=0 && tempArrayList.removeAll(arcList_To))
		{
			//only one element exists if everything is correct...
			String tempLoc=tempArrayList.iterator().next();
			
			//create a root locator
			Locator rootNode=new Locator(tempLoc,0);
			System.out.println("RootNode: "+rootNode.getLabel());
			return rootNode;
		}
		else
		{
			throw new NullPointerException("Unable to find the Root Node!");
		}
	}
	
	/*
	 * Imports the parentlabel and childlabel from arclist into the tree.
	 * Steps:
	 * -----
	 * 1.First ,the parentLabel and childLabel from arcList are converted into LabelNode.
	 * 2.then,it is passed to "insertNode(LabelNode t_parentNode,LabelNode t_childnode)". 
	 * 
	 * The entire arclist is iterated until all the labels are stored.
	 */
	public void importArcsIntoLabelTree()
	{
		Iterator<Arc> itr=null;
		Arc tempArc=null;
		
		String parentLabel=null;
		String childLabel=null;
		float order=0F;
		try
		{
			System.out.println("[IMPORT:ArcList-->LabelTree]:Started.Current Node Count in Tree="+labelTree.getNodeCount());
			if(arcList==null)
				throw new NullPointerException();
			
			int passCount=1;
			while(arcList.size()!=0)
			{
				System.out.println("PASS :"+passCount+" , ArcList size:"+arcList.size());
				itr=arcList.iterator();
				while(itr.hasNext())
				{
					tempArc=itr.next();
					
					//get the parent and child labels & its order
					parentLabel=tempArc.getParentLabel();
					childLabel=tempArc.getChildLabel();
					order=tempArc.getOrder();
					
					//start inserting each arc element to the LabelTree.
					if(labelTree.insertNode(new LabelNode(new Locator(parentLabel, 0)),new LabelNode(new Locator(childLabel,order))))
						itr.remove();//removes the element if successfully inserted into tree.
	
				}//inner-while
				passCount++;
			}//while
			
			System.out.println("[IMPORT:ArcList-->LabelTree]:Completed.Node Count in Tree="+labelTree.getNodeCount());
		}
		catch(NullPointerException e)
		{
			System.out.println("ERROR:Unable to import ArcList into Tree. ArcList is not created! "+e);
		}
		
	}
	
	public void buildLabelTree()
	{
		try
		{
			if(root_LocatorNode==null)
				throw new NullPointerException("Root Node is null.");
			
			//creates the labeltree with this rootnode
			labelTree=new LabelTree(new LabelNode(root_LocatorNode));
			this.importArcsIntoLabelTree();
		}
		catch(NullPointerException e)
		{
			System.out.println("Unable to create LabelTree!! "+e);
		}
	}
	public ArrayList<String> getLocatorList() 
	{
		return locatorList;
	}
	
	public void viewTree()
	{
		labelTree.displayEntireTree();
	}
	public static void main(String[] args)
	{
		System.out.println("****LabelTree Processor****");
		
		LabelTreeProcessor processor=new LabelTreeProcessor();
		processor.loadFile(new File("testfiles\\custom_pre2.xml"));

		//process a single xlink:presentation node
		processor.processPresentationNode("two");
		
		//building the label tree from the processed data
		processor.buildLabelTree();
		
		//displaying the tree struture
		processor.viewTree();
	}
}
