package etomica.virial.cluster2.mvc.view;

import etomica.virial.cluster2.mvc.*;

public class ClusterWizardController extends WizardController {

  @Override
  protected WizardView createWizard() {

    return new ClusterWizard("Cluster Creation Wizard");
  }

  @Override
  protected Action nextAction(ViewResponse response) {

    if (response.getStatus().isTerminated()) {
      return new DefaultWizardAction(ActionStatus.COMPLETE_SUCCESS);
    }
    else {
      return new DefaultWizardAction(ActionStatus.CONTINUE_SUCCESS);
    }
  }

  @Override
  protected WizardPageView createPageView(ActionResponse actionResponse, ViewResponse viewResponse) {

    if (actionResponse == null || viewResponse == null) {
      return new ClusterWizardPage1(this);
    }
    // successful action
    if (actionResponse.getStatus() == ActionStatus.CONTINUE_SUCCESS) {
      // back button from the view
      if (viewResponse.getStatus() == ViewStatus.CONTINUE_PRIOR) {
        if (viewResponse.getView() instanceof ClusterWizardPage5) {
          return new ClusterWizardPage4(this);
        }
        else if (viewResponse.getView() instanceof ClusterWizardPage4) {
          return new ClusterWizardPage3(this);
        }
        else if (viewResponse.getView() instanceof ClusterWizardPage3) {
          return new ClusterWizardPage2(this);
        }
        else if (viewResponse.getView() instanceof ClusterWizardPage2) {
          return new ClusterWizardPage1(this);
        }
      }
      // next button from the view
      else if (viewResponse.getStatus() == ViewStatus.CONTINUE_NEXT) {
        if (viewResponse.getView() instanceof ClusterWizardPage4) {
          return new ClusterWizardPage5(this);
        }
        else if (viewResponse.getView() instanceof ClusterWizardPage3) {
          return new ClusterWizardPage4(this);
        }
        else if (viewResponse.getView() instanceof ClusterWizardPage2) {
          return new ClusterWizardPage3(this);
        }
        else if (viewResponse.getView() instanceof ClusterWizardPage1) {
          return new ClusterWizardPage2(this);
        }
      }
    }
    return new ClusterWizardPage1(this);
  }

  @Override
  protected WizardState createWizardState() {

    return new ClusterWizardState();
  }
}
