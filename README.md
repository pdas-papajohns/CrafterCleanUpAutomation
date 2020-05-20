# CrafterCleanUpAutomation
Crafter Clean Up Automation

The application aims to delete/update the crafter content xml as required. It reads the product codes from a file and checks whether is 
there any dependency on the code, and if not it deletes the file. If there is any dependency of the code on any product group, and the 
product is not set as default, then it will remove the product's refernce from the product-group.
Now if the product is set as the default product, need to discuss how to automate it with the application.

The application expects there arguements:
1) The full path of the csv file containing unused product codes.
2) The folder location of the Crafter Content that requires clean-up.
3) The number of instance in which the application will run.
