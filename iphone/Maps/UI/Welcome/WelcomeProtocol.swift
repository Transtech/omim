protocol WelcomeProtocolBase: class {

  static var key: String { get }

  var pageIndex: Int! { get set }

  weak var pageController: WelcomePageController! { get set }

  func updateSize()

  weak var image: UIImageView! { get set }
  weak var alertTitle: UILabel! { get set }
  weak var alertText: UILabel! { get set }
  weak var nextPageButton: UIButton! { get set }
  weak var containerWidth: NSLayoutConstraint! { get set }
  weak var containerHeight: NSLayoutConstraint! { get set }
  weak var imageMinHeight: NSLayoutConstraint! { get set }
  weak var imageHeight: NSLayoutConstraint! { get set }
  weak var titleTopOffset: NSLayoutConstraint! { get set }
  weak var titleImageOffset: NSLayoutConstraint! { get set }
}

extension WelcomeProtocolBase {

  static var key: String { return "\(self)" + AppInfo.shared().bundleVersion! }

  static func controller(_ pageIndex: Int) -> UIViewController {
    let sb = Storyboard.Welcome.instance
    let id = String(describing: self)
    let vc = sb.instantiateViewController(withIdentifier: id)
    (vc as! Self).pageIndex = pageIndex
    return vc
  }

  func setup(image: UIImage, title: String, text: String, buttonTitle: String, buttonAction: Selector) {
    self.image.image = image
    alertTitle.text = title
    alertText.text = text
    nextPageButton.setTitle(buttonTitle, for: .normal)
    nextPageButton.addTarget(self, action: buttonAction, for: .touchUpInside)
  }

  func updateSize() {
    let size = pageController.view!.size
    let (width, height) = (size.width, size.height)
    let hideImage = (imageHeight.multiplier * height <= imageMinHeight.constant)
    titleImageOffset.priority = hideImage ? UILayoutPriorityDefaultLow : UILayoutPriorityDefaultHigh
    image.isHidden = hideImage
    containerWidth.constant = width
    containerHeight.constant = height
  }
}

protocol WelcomeProtocol: WelcomeProtocolBase {

  typealias ConfigBlock = (Self) -> Void

  static var pagesConfigBlocks: [ConfigBlock]! { get }

  func config()
}

extension WelcomeProtocol {

  static var pagesConfigBlocks: [ConfigBlock]! { return nil }

  static var pagesCount: Int { return pagesConfigBlocks.count }

  func config() { type(of: self).pagesConfigBlocks[pageIndex](self) }
}
