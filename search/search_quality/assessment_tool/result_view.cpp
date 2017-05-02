#include "search/search_quality/assessment_tool/result_view.hpp"

#include "search/result.hpp"
#include "search/search_quality/assessment_tool/helpers.hpp"

#include "base/stl_add.hpp"

#include <utility>
#include <vector>

#include <QtWidgets/QHBoxLayout>
#include <QtWidgets/QLabel>
#include <QtWidgets/QRadioButton>
#include <QtWidgets/QVBoxLayout>

using namespace std;

namespace
{
QLabel * CreateLabel(QWidget & parent)
{
  QLabel * label = new QLabel(&parent);
  label->setSizePolicy(QSizePolicy::Preferred, QSizePolicy::Minimum);
  label->setWordWrap(true);
  return label;
}

void SetText(QLabel & label, string const & text) {
  if (text.empty())
  {
    label.hide();
    return;
  }

  label.setText(ToQString(text));
  label.show();
}

string GetResultType(search::Sample::Result const & result)
{
  return strings::JoinStrings(result.m_types, ", ");
}
}  // namespace

ResultView::ResultView(string const & name, string const & type, string const & address,
                       QWidget & parent)
  : QWidget(&parent)
{
  Init();
  SetContents(name, type, address);
  setEnabled(false);
  setObjectName("result");
}

ResultView::ResultView(search::Result const & result, QWidget & parent)
  : ResultView(result.GetString(), result.GetFeatureType(), result.GetAddress(), parent)
{
}

ResultView::ResultView(search::Sample::Result const & result, QWidget & parent)
  : ResultView(strings::ToUtf8(result.m_name), GetResultType(result), string() /* address */,
               parent)
{
}

void ResultView::EnableEditing(Edits::RelevanceEditor && editor)
{
  m_editor = my::make_unique<Edits::RelevanceEditor>(std::move(editor));

  m_irrelevant->setChecked(false);
  m_relevant->setChecked(false);
  m_vital->setChecked(false);

  switch (m_editor->Get())
  {
  case Relevance::Irrelevant: m_irrelevant->setChecked(true); break;
  case Relevance::Relevant: m_relevant->setChecked(true); break;
  case Relevance::Vital: m_vital->setChecked(true); break;
  }

  setEnabled(true);
}

void ResultView::Update()
{
  if (m_editor && m_editor->HasChanges())
    setStyleSheet("#result {background: rgba(255, 255, 200, 50%)}");
  else
    setStyleSheet("");
}

void ResultView::Init()
{
  auto * layout = new QVBoxLayout(this /* parent */);
  layout->setSizeConstraint(QLayout::SetMinimumSize);
  setLayout(layout);

  m_name = CreateLabel(*this /* parent */);
  layout->addWidget(m_name);

  m_type = CreateLabel(*this /* parent */);
  m_type->setStyleSheet("QLabel { font-size : 8pt }");
  layout->addWidget(m_type);

  m_address = CreateLabel(*this /* parent */);
  m_address->setStyleSheet("QLabel { color : green ; font-size : 8pt }");
  layout->addWidget(m_address);

  {
    auto * group = new QWidget(this /* parent */);
    auto * groupLayout = new QHBoxLayout(group /* parent */);
    group->setLayout(groupLayout);

    m_irrelevant = CreateRatioButton("0", *groupLayout);
    m_relevant = CreateRatioButton("+1", *groupLayout);
    m_vital = CreateRatioButton("+2", *groupLayout);

    layout->addWidget(group);
  }
}

void ResultView::SetContents(string const & name, string const & type, string const & address)
{
  SetText(*m_name, name);
  SetText(*m_type, type);
  SetText(*m_address, address);

  m_irrelevant->setChecked(false);
  m_relevant->setChecked(false);
  m_vital->setChecked(false);
}

QRadioButton * ResultView::CreateRatioButton(string const & text, QLayout & layout)
{
  QRadioButton * radio = new QRadioButton(QString::fromStdString(text), this /* parent */);
  layout.addWidget(radio);

  connect(radio, &QRadioButton::toggled, this, &ResultView::OnRelevanceChanged);
  return radio;
}

void ResultView::OnRelevanceChanged()
{
  if (!m_editor)
    return;

  auto relevance = Relevance::Irrelevant;
  if (m_irrelevant->isChecked())
    relevance = Relevance::Irrelevant;
  else if (m_relevant->isChecked())
    relevance = Relevance::Relevant;
  else if (m_vital->isChecked())
    relevance = Relevance::Vital;

  m_editor->Set(relevance);
}